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
import com.sun.identity.common.PeriodicCleanUpMap;

import javax.servlet.http.HttpServletRequest;

public class RequestHelper {
    @JsonProperty
    private String operation;

    @JsonProperty
    private String sid;
    @JsonIgnore
    private String encKey;
    @JsonIgnore
    private String encData;
    @JsonProperty
    private String encAuth;
    @JsonProperty
    private String traceId;
    @JsonProperty
    private String marchantName;
    @JsonProperty
    private boolean readSSN;

    public static RequestHelper getHelper(HttpServletRequest request, PeriodicCleanUpMap cache) {
        RequestHelper reqHelper = new RequestHelper();
        reqHelper.operation = request.getParameter("operation");
        reqHelper.sid = request.getParameter("sid");
        reqHelper.encKey = request.getParameter("encKey");
        reqHelper.encData = request.getParameter("encData");
        reqHelper.encAuth = request.getParameter("encAuth");
        if (cache.containsKey(reqHelper.getSid())) {
            DataHelper dataHelper = (DataHelper)cache.get(reqHelper.getSid());
            reqHelper.traceId = dataHelper.getTraceId();
            reqHelper.marchantName = dataHelper.getMarchantName();
            reqHelper.readSSN = dataHelper.isReadSSN();
        }
        return reqHelper;
    }

    @JsonIgnore
    public boolean isAuthRequest() {
        return operation.equals("initAuth");
    }

    @JsonIgnore
    public boolean isVerifyRequest() {
        return operation.equals("verifyAuth");
    }

    @JsonIgnore
    public boolean isError() {
        return operation.equals("handleError");
    }

    public String toString() {
        try {
            return new ObjectMapper()
                    .writer()
                    .writeValueAsString(this);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public String getOperation() {
        return operation;
    }

    public String getSid() {
        return sid;
    }

    public String getEncKey() {
        return encKey;
    }

    public String getEncData() {
        return encData;
    }

    public String getEncAuth() {
        return encAuth;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getMarchantName() {
        return marchantName;
    }

    public boolean isReadSSN() {
        return readSSN;
    }
}
