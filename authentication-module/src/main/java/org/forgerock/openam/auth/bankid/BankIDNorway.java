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

import javax.crypto.SecretKey;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.*;

public class BankIDNorway extends AMLoginModule {
    private static final Debug debug = Debug.getInstance("BankIDNorway");
    public static final String WEB_CLIENT = "WEB_CLIENT";
    public static final String MOBILE_CLIENT = "MOBILE_CLIENT";

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
        bundle = amCache.getResBundle("amBankIDNorway", getLoginLocale());
        config = new BankIDConfiguration();

        config.clientType = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-client-type");
        config.merchantName = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-name");
        config.merchantWebAddress = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-web-address");
        config.merchantURL = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-url");
        config.merchantFeDomain = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-fe-domain");
        config.merchantFeAncestors = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-fe-ancestors");
        config.merchantKeystore = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-keystore");
        config.merchantKeystorePassword = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-keystore-pwd");
        try {
            Set<String> policies = CollectionHelper.getMapSetThrows(options, "iplanet-am-auth-bankidnorway-merchant-granted-policies");

            if (policies.contains("ALL")) {
                config.merchantGrantedPolicies = "ALL";
            } else {
                config.merchantGrantedPolicies = StringUtils.join(policies, ",");
            }
        } catch (ValueNotFoundException e) {
            config.merchantGrantedPolicies = "ALL";
        }

        config.retrieveSSN = CollectionHelper.getBooleanMapAttr(options, "iplanet-am-auth-bankidnorway-read-ssn", false);

        config.propsMappings = new HashMap<String, String>();
        try {
            Set<String> mappings = CollectionHelper.getMapSetThrows(options, "iplanet-am-auth-bankidnorway-merchant-mapping-list");
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

    private String getLocale() {
        String lang = getLoginLocale().getLanguage();
        return lang.equals("no") || lang.equals("nb") || lang.equals("nn") ? "nb" : "en";
    }

    private int initWebClientSession()  throws LoginException {
        String sessionId = java.util.UUID.randomUUID().toString();
        dataHelper = new DataHelper(sessionId);

        try {
            HttpServletRequest request = getHttpServletRequest();

            BIDFactory factory = BIDFactory.getInstance();
            MerchantConfig mConfig = new MerchantConfig();

            mConfig.setGrantedPolicies(config.merchantGrantedPolicies);
            mConfig.setKeystorePassword(config.merchantKeystorePassword);
            mConfig.setMerchantKeystore(config.merchantKeystore);
            mConfig.setMerchantName(config.merchantName);
            mConfig.setWebAddresses(config.merchantWebAddress); //"bankid-am.openrock.org,192.168.0.1"
            factory.registerBankIDContext(mConfig);

            BIDFacade bankIDFacade = factory.getFacade(config.merchantName);

            InitSessionInfo initSessionInfo = new InitSessionInfo();
            initSessionInfo.setAction("auth");
            initSessionInfo.setUserAgent(request.getHeader("user-agent"));
            initSessionInfo.setClientVersion(CLIENT_VERSION);
            initSessionInfo.setMerchantURL(config.merchantURL);

            initSessionInfo.setLocaleId(getLocale());
            initSessionInfo.setSid(sessionId);
            initSessionInfo.setSuppressBroadcast("N");
            initSessionInfo.setCertType(config.merchantGrantedPolicies);
            initSessionInfo.setTimeout(config.timeout);
            initSessionInfo.setMerchantFEDomain(config.merchantFeDomain);

            initSessionInfo = bankIDFacade.initSession(initSessionInfo);

            // Return parameters of interest
            String clientID = initSessionInfo.getClientID();
            String helperURI = initSessionInfo.getHelperURI();
            traceId = initSessionInfo.getTraceID();

            dataHelper.setHelperURL(helperURI);
            dataHelper.setClientId(clientID);
            dataHelper.setTraceId(traceId);
            dataHelper.setMerchantName(config.merchantName);
            dataHelper.setReadSSN(config.retrieveSSN);

            if (debug.messageEnabled()) {
                debug.message("Helper data: " + dataHelper.toString());
            }
            requestCache.put(sessionId, dataHelper);

            customizeCallbacks(STATE_AUTH);
            return STATE_AUTH;
        } catch(BIDException be) {
            ResponseHelper responseHelper = new ResponseHelper("" + be.getErrorCode());
            responseCache.put(sessionId, responseHelper);
            customizeCallbacks(STATE_ERROR);
            return STATE_ERROR;
        }
    }

    private int initMobileClientSession()  throws LoginException {
        return STATE_AUTH;
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

    private void customizeCallbacks(int state) throws LoginException {
        switch (state) {
            case STATE_AUTH: {
                StringBuffer js = new StringBuffer();
                js.append("var dataHelper = ")
                        .append(dataHelper.toString()).append(System.lineSeparator())
                        .append("initiateClient(dataHelper);");

                ScriptTextOutputCallback stoc = new ScriptTextOutputCallback(js.toString());
                replaceCallback(STATE_AUTH, 1, stoc);
                break;
            }
            case STATE_ERROR: {
                ResponseHelper responseHelper = (ResponseHelper)responseCache.get(dataHelper.getSessionId());

                substituteHeader(STATE_ERROR, bundle.getString("msg.authFailed"));
                String errorCode = responseHelper.getErrorCode();

                String userMsg = bundle.containsKey(errorCode)
                        ? bundle.getString(errorCode)
                        : bundle.getString("msg.genericError") + errorCode;
                TextOutputCallback toc = new TextOutputCallback(TextOutputCallback.ERROR, userMsg);
                replaceCallback(STATE_ERROR, 0, toc);

                ConfirmationCallback cc = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                        new String [] {bundle.getString("msg.tryAgain"), bundle.getString("msg.cancel")}, 0);
                replaceCallback(STATE_ERROR, 1, cc);
                break;
            }
        }

    }

    private void cleanup() {
        responseCache.remove(dataHelper.getSessionId());
        requestCache.remove(dataHelper.getSessionId());
        dataHelper = null;
        userName = null;
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        switch (state) {
            case STATE_SESSION: {
                if (debug.messageEnabled()) {
                    debug.message("BankIDNorway::processing STATE_SESSION...");
                }
                if (config.clientType.equals(WEB_CLIENT)) {
                    return initWebClientSession();
                } else {
                    return initMobileClientSession();
                }
            }
            case STATE_AUTH: {
                if (debug.messageEnabled()) {
                    debug.message("BankIDNorway::processing STATE_AUTH...");
                }
                if (responseCache.containsKey(dataHelper.getSessionId())) {
                    ResponseHelper responseHelper = (ResponseHelper)responseCache.get(dataHelper.getSessionId());

                    if (responseHelper.isError()) {
                        customizeCallbacks(STATE_ERROR);
                       return STATE_ERROR;
                    }

                    Map attrs = createUserAttribtues(responseHelper);
                    setUserAttributes(attrs);

                    userName = (config.retrieveSSN && responseHelper.getSsn() != null)
                            ? responseHelper.getSsn()
                            : responseHelper.getUid();
                }
                return ISAuthConstants.LOGIN_SUCCEED;
            }
            case STATE_ERROR: {
                if (debug.messageEnabled()) {
                    debug.message("BankIDNorway::processing STATE_ERROR...");
                }
                cleanup();

                int action = ((ConfirmationCallback)callbacks[1]).getSelectedIndex();
                if (action == 0) {
                    return STATE_SESSION;
                } else {
                    return ISAuthConstants.LOGIN_IGNORE;
                }
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

    private static void initAuthentication(RequestHelper helper, PrintWriter out) {
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

            out.println(responseToClient);
        } catch(BIDException be) {
            handleException(helper, be, out);
        }

    }

    private static void verifyAuthentication(RequestHelper helper, PrintWriter out) {
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

            String responseToClient = bankIDFacade.verifyTransactionResponse(sessionData);

            out.println(responseToClient);
        } catch(BIDException be) {
            handleException(helper, be, out);
        }

    }

    private static void handleError(RequestHelper helper, PrintWriter out) {
        DataHelper dataHelper = (DataHelper)requestCache.remove(helper.getSid());
        BIDSessionData sessionData = dataHelper.getSessaionData();
        BIDFactory factory = BIDFactory.getInstance();

        try {
            BIDFacade bankIDFacade = factory.getFacade(helper.getMarchantName());
            bankIDFacade.verifyTransactionRequest(
                    helper.getOperation(),
                    helper.getEncKey(),
                    helper.getEncData(),
                    helper.getEncAuth(),
                    helper.getSid(),
                    sessionData);
            String errorCode = sessionData.getErrCode();
            ResponseHelper responseHelper = new ResponseHelper(errorCode);

            responseCache.put(helper.getSid(), responseHelper);

            if (debug.messageEnabled()) {
                debug.message("User information: " + responseHelper.toString());
            }

            String responseToClient = bankIDFacade.verifyTransactionResponse(sessionData);
            out.println(responseToClient);

        } catch (BIDException be) {
            handleException(helper, be, out);
        }

    }

    private static void handleException(RequestHelper helper, BIDException ex, PrintWriter out) {
        DataHelper dataHelper = (DataHelper)requestCache.remove(helper.getSid());
        BIDSessionData sessionData = dataHelper.getSessaionData();
        BIDFactory factory = BIDFactory.getInstance();


        sessionData.setErrCode("" + ex.getErrorCode());
        ResponseHelper responseHelper = new ResponseHelper(sessionData.getErrCode());

        responseCache.put(helper.getSid(), responseHelper);

        if (debug.messageEnabled()) {
            debug.message("User information: " + responseHelper.toString());
        }


        // If the key can not be found, a response must be sent, unencrypted
        SecretKey key = sessionData.getSessionKey();
        if (key == null) {
            // Unencrypted response
            out.println("errCode=" + ex.getErrorCode());
        } else {
            // Encrypted response
            try {
                BIDFacade bankIDFacade = factory.getFacade(helper.getMarchantName());

                String responseToClient = bankIDFacade.verifyTransactionResponse(sessionData);
                out.println(responseToClient);
            } catch (BIDException ex2) {
                if (debug.errorEnabled()) {
                    debug.error("Error verifying transaction.", ex2);
                };
            }
        }
    }

    public static void processRequest(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        RequestHelper reqHelper = RequestHelper.getHelper(request, requestCache);
        if (debug.messageEnabled()) {
            debug.message("Request helper: " + reqHelper.toString());
        }
        if (reqHelper.isAuthRequest()) {
            initAuthentication(reqHelper, out);
        } else if (reqHelper.isVerifyRequest()) {
            verifyAuthentication(reqHelper, out);
        } else if (reqHelper.isError()) {
            handleError(reqHelper, out);
        }
    }
}
