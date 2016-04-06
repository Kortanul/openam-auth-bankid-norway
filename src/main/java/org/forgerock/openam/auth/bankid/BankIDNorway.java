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

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
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
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.auth.bankid.helper.DataHelper;
import org.forgerock.openam.auth.bankid.helper.RequestHelper;
import org.forgerock.openam.auth.bankid.helper.ResponseHelper;

import javax.crypto.SecretKey;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.*;

public class BankIDNorway extends AMLoginModule {
    private static final Debug debug = Debug.getInstance("BankIDNorway");
    private static final String RB_BUNDLE_NAME = "amBankIDNorway";
    private final static String CLIENT_VERSION = "2.1";

    public static final String WEB_CLIENT = "WEB_CLIENT";
    public static final String MOBILE_CLIENT = "MOBILE_CLIENT";

    private ResourceBundle bundle;

    public static final PeriodicCleanUpMap requestCache = new PeriodicCleanUpMap(60000L, 300000L);
    public static final PeriodicCleanUpMap responseCache = new PeriodicCleanUpMap(60000L, 300000L);

    private static final int STATE_SID = 1;
    private static final int STATE_INIT = 2;
    private static final int STATE_AUTHENTICATE = 3;
    private static final int STATE_MOBILE_CLIENT_FORM = 4;

    private BankIDConfiguration config;
    private DataHelper dataHelper;

    //
    private String traceId;
    private String userName;


    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        if (debug.messageEnabled()) {
            debug.message("BankID Norway::init");
        }

        bundle = amCache.getResBundle(RB_BUNDLE_NAME, getLoginLocale());
        config = new BankIDConfiguration();

        String clientType = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-client-type");
        if (WEB_CLIENT.equals(clientType)) {
            config.clientType = ClientType.WEB;
        } else if (MOBILE_CLIENT.equals(clientType)) {
            config.clientType = ClientType.MOBILE;
        }

        config.merchantName = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-name");
        config.merchantWebAddress = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-web-address");
        config.merchantURL = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-url");
        config.merchantFeDomain = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-fe-domain");
        config.merchantFeAncestors = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-fe-ancestors");
        config.merchantKeystore = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-keystore");
        config.merchantKeystorePassword = CollectionHelper.getMapAttr(options, "iplanet-am-auth-bankidnorway-merchant-keystore-pwd");

        try {
            Set<String> policies = CollectionHelper.getMapSetThrows(options,"iplanet-am-auth-bankidnorway-merchant-granted-policies");

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

    private String getBankIdLocale() {
        String lang = getLoginLocale().getLanguage();
        return lang.equals("no") || lang.equals("nb") || lang.equals("nn") ? "nb" : "en";
    }

    private BIDFacade getMerchantFacade() throws BIDException {
        BIDFactory factory = BIDFactory.getInstance();
        MerchantConfig mConfig = new MerchantConfig();

        mConfig.setGrantedPolicies(config.merchantGrantedPolicies);
        mConfig.setKeystorePassword(config.merchantKeystorePassword);
        mConfig.setMerchantKeystore(config.merchantKeystore);
        mConfig.setMerchantName(config.merchantName);
        mConfig.setWebAddresses(config.merchantWebAddress); //"bankid-am.openrock.org,192.168.0.1"
        factory.registerBankIDContext(mConfig);

        return factory.getFacade(config.merchantName);
    }

    private int initWebClientSession()  throws LoginException {
        String sessionId = dataHelper.getSessionId();

        try {
            HttpServletRequest request = getHttpServletRequest();

            BIDFacade bankIDFacade = getMerchantFacade();

            InitSessionInfo initSessionInfo = new InitSessionInfo();
            initSessionInfo.setAction("auth");
            initSessionInfo.setUserAgent(request.getHeader("user-agent"));
            initSessionInfo.setClientVersion(CLIENT_VERSION);
            initSessionInfo.setMerchantURL(config.merchantURL);

            initSessionInfo.setLocaleId(getBankIdLocale());
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
            dataHelper.setClientType(ClientType.WEB);

            if (debug.messageEnabled()) {
                debug.message("Helper data: " + dataHelper.toString());
            }
            requestCache.put(sessionId, dataHelper);

            customizeCallbacks(STATE_AUTHENTICATE);
            return STATE_AUTHENTICATE;
        } catch(BIDException be) {
            throw new AuthLoginException(RB_BUNDLE_NAME, "err." + be.getErrorCode(), null);
        }
    }

    private int initMobileClientSession(String mobile, String dob)  throws LoginException {
        String sessionId = dataHelper.getSessionId();

        if (mobile == null || mobile.isEmpty()) {
            customizeCallbacks(STATE_MOBILE_CLIENT_FORM);
            return STATE_MOBILE_CLIENT_FORM;
        }

        dataHelper.setMerchantName(config.merchantName);
        dataHelper.setReadSSN(config.retrieveSSN);
        dataHelper.setClientType(ClientType.MOBILE);
        dataHelper.setNextURL(config.nextURL);

        try {
            BIDFacade bankIDFacade = getMerchantFacade();
            String merchantReference = bankIDFacade.generateMerchantReference("no_NO");

            MobileInfo mobileInfo = new MobileInfo();
            mobileInfo.setAction("auth");
            mobileInfo.setMerchantReference(merchantReference);
            mobileInfo.setPhoneNumber(mobile);
            mobileInfo.setPhoneAlias(dob);
            mobileInfo.setSid(sessionId);
            mobileInfo.setUrl(config.merchantURL);
            mobileInfo.setCertType(config.getMerchantGrantedPolicies());

            if (debug.messageEnabled()) {
                debug.message("Helper data: " + dataHelper.toString());
            }
            requestCache.put(sessionId, dataHelper);


            // be aware that the following call can cause a hang for up to 3 minutes while the
            // end-user processes messages on the cell phone.At the same time the web application
            // must expect callbacks on the specified URL
            TransactionAndStatus ts = bankIDFacade.requestMobileAction(mobileInfo);
            if ("0".equals(ts.getStatusCode())) {
                return STATE_AUTHENTICATE;
            } else {
                if (debug.errorEnabled()) {
                    debug.error("Can't authenticate, status code: " + ts.getStatusCode());
                }
                requestCache.remove(sessionId);
                throw new AuthLoginException(RB_BUNDLE_NAME, "err." + ts.getStatusCode(), null);
            }

        } catch(BIDException be) {
            if (debug.errorEnabled()) {
                debug.error("Can't initialize mobile client", be);
            }
            throw new AuthLoginException(RB_BUNDLE_NAME, "err." + be.getErrorCode(), null);
        }
    }

    private Map createUserAttribtues(final ResponseHelper responseHelper) {
        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();

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
            case STATE_INIT: {
                ScriptTextOutputCallback stoc = new ScriptTextOutputCallback("var sid = " + dataHelper.getSessionId());
                replaceCallback(STATE_INIT, 0, stoc);

                break;
            }
            case STATE_AUTHENTICATE: {
                StringBuffer js = new StringBuffer();
                js.append("var dataHelper = ")
                        .append(dataHelper.toString()).append(System.lineSeparator())
                        .append("initiateClient(dataHelper);");

                ScriptTextOutputCallback stoc = new ScriptTextOutputCallback(js.toString());
                replaceCallback(STATE_AUTHENTICATE, 0, stoc);

                break;
            }
            case STATE_MOBILE_CLIENT_FORM: {
                substituteHeader(STATE_MOBILE_CLIENT_FORM, bundle.getString("mobile.form.header"));

                NameCallback phoneNC = new NameCallback(bundle.getString("cb.phone"));
                replaceCallback(STATE_MOBILE_CLIENT_FORM, 0, phoneNC);

                NameCallback dobNC = new NameCallback(bundle.getString("cb.dob"));
                replaceCallback(STATE_MOBILE_CLIENT_FORM, 1, dobNC);
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
            case STATE_SID : {
                if (debug.messageEnabled()) {
                    debug.message("BankIDNorway::processing STATE_SID...");
                }
                String sessionId = java.util.UUID.randomUUID().toString();
                dataHelper = new DataHelper(sessionId);
                customizeCallbacks(STATE_INIT);
                return STATE_INIT;
            }
            case STATE_INIT: {
                if (debug.messageEnabled()) {
                    debug.message("BankIDNorway::processing STATE_INIT...");
                }

                //TODO: detect existing session

                if (config.clientType == ClientType.WEB) {
                    //TODO: get user SSN from the session
                    return initWebClientSession();
                } else {
                    //TODO: get phone and alias from the session
                    String mobile = null;
                    String dob = null;
                    return initMobileClientSession(mobile, dob);
                }
            }
            case STATE_AUTHENTICATE: {
                if (debug.messageEnabled()) {
                    debug.message("BankIDNorway::processing STATE_AUTHENTICATION...");
                }
                if (responseCache.containsKey(dataHelper.getSessionId())) {
                    ResponseHelper responseHelper = (ResponseHelper)responseCache.get(dataHelper.getSessionId());

                    if (responseHelper.isError()) {
                        throw new AuthLoginException(RB_BUNDLE_NAME, "err." + responseHelper.getErrorCode(), null);
                    }

                    Map attrs = createUserAttribtues(responseHelper);
                    setUserAttributes(attrs);

                    userName = (config.retrieveSSN && responseHelper.getSsn() != null)
                            ? responseHelper.getSsn()
                            : responseHelper.getUid();
                }
                return ISAuthConstants.LOGIN_SUCCEED;
            }
            case STATE_MOBILE_CLIENT_FORM: {
                if (debug.messageEnabled()) {
                    debug.message("BankIDNorway::processing STATE_MOBILE_CLIENT_FORM...");
                }

                String mobile = ( (NameCallback) callbacks[0]).getName();
                String dob = ( (NameCallback) callbacks[1]).getName();

                return initMobileClientSession(mobile, dob);
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
        DataHelper dataHelper = (DataHelper)requestCache.get(helper.getSid());

        BIDSessionData sessionData = (dataHelper.getClientType() == ClientType.WEB)
                ? new BIDSessionData(helper.getTraceId())
                : new BIDSessionData();

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

            if (dataHelper.getClientType() == ClientType.MOBILE) {
                sessionData.setNextURL(dataHelper.getNextURL());
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
