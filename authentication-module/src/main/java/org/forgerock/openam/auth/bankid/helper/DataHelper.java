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

package org.forgerock.openam.auth.bankid.helper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.bbs.server.vos.BIDSessionData;
import org.forgerock.openam.auth.bankid.ClientType;

public class DataHelper {
    /* COMMON PROPERTIES */

    @JsonProperty("sid")
    private String sessionId;

    @JsonProperty("merchant")
    private String merchantName;

    @JsonProperty("readSSN")
    private boolean readSSN;

    @JsonIgnore
    private BIDSessionData sessaionData;

    @JsonProperty("clientType")
    private ClientType clientType;

    /* WEB CLIENT SPECIFIC */

    @JsonProperty("helperURL")
    private String helperURL;

    @JsonProperty("cid")
    private String clientId;

    @JsonProperty("tid")
    private String traceId;

    /* MOBILE CLIENT SPECIFIC */

    @JsonProperty("nextURL")
    private String nextURL;


    public DataHelper(String sessionid) {
        this.sessionId = sessionid;
    }

    public String toString() {
        try {
            return new ObjectMapper().writer().writeValueAsString(this);
        } catch (Exception ex) {
            return "{}";
        }

    }

    public String getHelperURL() {
        return helperURL;
    }

    public void setHelperURL(String helperURL) {
        this.helperURL = helperURL;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    public BIDSessionData getSessaionData() {
        return sessaionData;
    }

    public void setSessaionData(BIDSessionData sessaionData) {
        this.sessaionData = sessaionData;
    }

    public boolean isReadSSN() {
        return readSSN;
    }

    public void setReadSSN(boolean readSSN) {
        this.readSSN = readSSN;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getNextURL() {
        return nextURL;
    }

    public void setNextURL(String nextURL) {
        this.nextURL = nextURL;
    }
}
