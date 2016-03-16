package org.forgerock.openam.auth.bankid;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.datastruct.ValueNotFoundException;
import com.sun.identity.shared.debug.Debug;
import no.bbs.server.constants.JServerConstants;
import no.bbs.server.exception.BIDException;
import no.bbs.server.implementation.BIDFacade;
import no.bbs.server.implementation.BIDFactory;
import no.bbs.server.vos.*;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.auth.bankid.helper.DataHelper;
import org.forgerock.openam.auth.bankid.helper.RequestHelper;
import org.forgerock.openam.auth.bankid.helper.ResponseHelper;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.*;

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file at legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
public class BankIDNorway extends AMLoginModule {
    private static final Debug debug = Debug.getInstance("BankIDNorway");
    private ResourceBundle bundle;

    public static final PeriodicCleanUpMap requestCache = new PeriodicCleanUpMap(60000L, 300000L);
    public static final PeriodicCleanUpMap responseCache = new PeriodicCleanUpMap(60000L, 300000L);

    private static final int STATE_SESSION = 1;
    private static final int STATE_AUTH = 2;
    private static final int STATE_ERROR = 3;

    private BankIDConfiguration config;
    private DataHelper dataHelper;

    private final static String CLIENT_VERSION = "2.1";

    //
    private String traceId;
    private String userName;


    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        if (debug.messageEnabled()) {
            debug.message("BankID Norway::init");
        }
//        bundle = amCache.getResBundle("BankIDNorway", getLoginLocale());
        config = new BankIDConfiguration();
        config.marchantName = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-marchant-name");
        config.marchantWebAddress = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-marchant-web-address");
        config.marchantURL = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-marchant-url");
        config.marchantFeDomain = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-marchant-fe-domain");
        config.marchantFeAncestors = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-marchant-fe-ancestors");
        config.marchantKeystore = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-marchant-keystore");
        config.marchantKeystorePassword = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-marchant-keystore-pwd");
        try {
            Set<String> policies = CollectionHelper.getMapSetThrows(options, "iplanet-am-auth-bankidnorway-marchant-granted-policies");

            if (policies.contains("ALL")) {
                config.marchantGrantedPolicies = "ALL";
            } else {
                config.marchantGrantedPolicies = StringUtils.join(policies, ",");
            }
        } catch (ValueNotFoundException e) {
            config.marchantGrantedPolicies = "ALL";
        }

        config.retrieveSSN = CollectionHelper.getBooleanMapAttr(options, "iplanet-am-auth-bankidnorway-read-ssn", false);

        config.propsMappings = new HashMap<String, String>();
        try {
            Set<String> mappings = CollectionHelper.getMapSetThrows(options, "iplanet-am-auth-bankidnorway-marchant-mapping-list");
            for (String mapping : mappings) {
                String split[] = mapping.split("=", 2);
                config.propsMappings.put(split[0], split[1]);
            }
        } catch (Exception e) {
            if (debug.errorEnabled()) {
                debug.error("Can't process mappings", e);
            }
            config.propsMappings.put(config.retrieveSSN ? "ssn" : "uid", "uid");

        }
        config.nextURL = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-next-url");
        config.timeout = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-timeout");
        config.withCredentials = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-with-credentials");
        config.sessionTimeout = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-client-session-timeout");

        if (debug.messageEnabled()) {
            debug.message("Configuration: " + config.toString());
        }

    }

    private void initSession()  throws LoginException {
        String sessionId = java.util.UUID.randomUUID().toString();

        try {
            HttpServletRequest request = getHttpServletRequest();

            BIDFactory factory = BIDFactory.getInstance();
            MerchantConfig mConfig = new MerchantConfig();

            /*
             * read below values from auth module config
             */
            mConfig.setGrantedPolicies(config.marchantGrantedPolicies);
            mConfig.setKeystorePassword(config.marchantKeystorePassword);
            mConfig.setMerchantKeystore(config.marchantKeystore);
            mConfig.setMerchantName(config.marchantName);
            mConfig.setWebAddresses(config.marchantWebAddress); //"bankid-am.openrock.org,192.168.0.1"
            factory.registerBankIDContext(mConfig);

            BIDFacade bankIDFacade = factory.getFacade(config.marchantName);


            InitSessionInfo initSessionInfo = new InitSessionInfo();
            initSessionInfo.setAction("auth");
            initSessionInfo.setUserAgent(request.getHeader("user-agent"));
            initSessionInfo.setClientVersion(CLIENT_VERSION);
            initSessionInfo.setMerchantURL(config.marchantURL);

            initSessionInfo.setLocaleId("en");
            initSessionInfo.setSid(sessionId);
            initSessionInfo.setSuppressBroadcast("N");
            initSessionInfo.setCertType(config.marchantGrantedPolicies);
            initSessionInfo.setTimeout(config.timeout);
            initSessionInfo.setMerchantFEDomain(config.marchantFeDomain);

            initSessionInfo = bankIDFacade.initSession(initSessionInfo);

            // Return parameters of interest
            String clientID = initSessionInfo.getClientID();
            String helperURI = initSessionInfo.getHelperURI();
            traceId = initSessionInfo.getTraceID();

            dataHelper = new DataHelper(helperURI, clientID, sessionId, traceId);
            dataHelper.setMarchantName(config.marchantName);
            dataHelper.setReadSSN(config.retrieveSSN);

            if (debug.messageEnabled()) {
                debug.message("Helper data: " + dataHelper.toString());
            }
            requestCache.put(sessionId, dataHelper);

            StringBuffer js = new StringBuffer();
            js.append("var dataHelper = ")
                    .append(dataHelper.toString()).append(System.lineSeparator())
                    .append("initiateClient(dataHelper);");

            ScriptTextOutputCallback stoc = new ScriptTextOutputCallback(js.toString());
            replaceCallback(STATE_AUTH, 1, stoc);

        } catch(BIDException be) {
            // Handle Error Situation
            debug.error("initSession()", be);
            requestCache.remove(sessionId);
        }
    }

    private Map createUserAttribtues(final ResponseHelper responseHelper) {
        Map attrs = new HashedMap();

        if (config.propsMappings.containsKey("SSN") && responseHelper.getSsn() != null) {
            attrs.put(config.propsMappings.get("SSN"), new HashSet<String>() {{
                add(responseHelper.getSsn());
            }});
        }
        if (config.propsMappings.containsKey("UID") && responseHelper.getUid() != null) {
            attrs.put(config.propsMappings.get("UID"), new HashSet<String>() {{
                add(responseHelper.getUid());
            }});
        }
        if (config.propsMappings.containsKey("CN") && responseHelper.getCn() != null) {
            attrs.put(config.propsMappings.get("CN"), new HashSet<String>() {{
                add(responseHelper.getCn());
            }});
        }

        //static mapping
        if (responseHelper.getSn() != null) {
            attrs.put("sn", new HashSet<String>() {{
                add(responseHelper.getSn());
            }});
        }

        return attrs;
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        if (debug.messageEnabled()) {
            debug.message("BankID Norway::process, state is " + state);
        }
        switch (state) {
            case STATE_SESSION: {
                initSession();
                return STATE_AUTH;
            }
            case STATE_AUTH: {
                if (responseCache.containsKey(dataHelper.getSessionId())) {
                    ResponseHelper responseHelper = (ResponseHelper)responseCache.get(dataHelper.getSessionId());
                    Map attrs = createUserAttribtues(responseHelper);
                    setUserAttributes(attrs);

                    userName = (config.retrieveSSN && responseHelper.getSsn() != null)
                            ? responseHelper.getSsn()
                            : responseHelper.getUid();
                }
                return ISAuthConstants.LOGIN_SUCCEED;
            }
            case STATE_ERROR: {

                break;
            }
            default: {

            }
        }
        return 0;
    }

    @Override
    public Principal getPrincipal() {
        return new BankIDPrincipal(userName);
    }

    private static void initAuthentication(RequestHelper helper, HttpServletResponse response, PrintWriter out) {
        BIDSessionData sessionData = new BIDSessionData(helper.getTraceId());
        DataHelper dataHelper = (DataHelper)requestCache.get(helper.getSid());
        dataHelper.setSessaionData(sessionData);

        BIDFactory factory = BIDFactory.getInstance();

        try {
            BIDFacade bankIDFacade = factory.getFacade(helper.getMarchantName());
            String responseToClient = bankIDFacade.initTransaction(
                    helper.getOperation(),
                    helper.getEncKey(),
                    helper.getEncData(),
                    helper.getEncAuth(),
                    helper.getSid(),
                    sessionData);

            //TODO: add BankID FOI to the config
            response.setHeader("Access-Control-Allow-Origin", "https://csfe-preprod.bankid.no");
            response.setHeader("Access-Control-Allow-Credentials", "true");

            out.println(responseToClient);
        } catch(BIDException be) {
            String errorMessage = be.getMessage();
            int errorCode = be.getErrorCode();

            //TODO: what should we do here

        }

    }

    private static void verifyAuthentication(RequestHelper helper, HttpServletResponse response, PrintWriter out) {
        DataHelper dataHelper = (DataHelper)requestCache.remove(helper.getSid());
        BIDSessionData sessionData = dataHelper.getSessaionData();
        BIDFactory factory = BIDFactory.getInstance();

        try {
            if (helper.isReadSSN()) {
                ArrayList<String> additionalInfos = new ArrayList<String>();
                additionalInfos.add(JServerConstants.LABEL_OID_OCSP_SSN);
                sessionData.setAdditionalInfoList(additionalInfos);
            }

            BIDFacade bankIDFacade = factory.getFacade(helper.getMarchantName());
            bankIDFacade.verifyTransactionRequest(
                    helper.getOperation(),
                    helper.getEncKey(),
                    helper.getEncData(),
                    helper.getEncAuth(),
                    helper.getSid(),
                    sessionData);

            CertificateStatus certStatus = sessionData.getCertificateStatus();
            CertificateInfo certInfo = bankIDFacade.getCertificateInfo(bankIDFacade
                    .getPKCS7Info(sessionData.getClientSignature())
                    .getSignerCertificate());

            ResponseHelper responseHelper = new ResponseHelper(
                    certStatus.getAddInfoSSN(),
                    certInfo.getUniqueId(),
                    certInfo.getCommonName() );

            responseCache.put(helper.getSid(), responseHelper);

            if (debug.messageEnabled()) {
                debug.message("User information: " + responseHelper.toString());
            }

            //TODO: add BankID FOI to the config
            response.setHeader("Access-Control-Allow-Origin", "https://csfe-preprod.bankid.no");
            response.setHeader("Access-Control-Allow-Credentials", "true");

            String responseToClient = bankIDFacade.verifyTransactionResponse(sessionData);

            out.println(responseToClient);
        } catch(BIDException be) {
            String errorMessage = be.getMessage();
            int errorCode = be.getErrorCode();

            //TODO: what should we do here
            if (debug.errorEnabled()) {
                debug.error("can't verify authentication", be);
            }

        }

    }

    public static void processRequest(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        RequestHelper reqHelper = RequestHelper.getHelper(request, requestCache);
        if (debug.messageEnabled()) {
            debug.message("Request helper: " + reqHelper.toString());
        }
        if (reqHelper.isAuthRequest()) {
            initAuthentication(reqHelper, response, out);
        } else if (reqHelper.isVerifyRequest()) {
            verifyAuthentication(reqHelper, response, out);
        }
    }
}
