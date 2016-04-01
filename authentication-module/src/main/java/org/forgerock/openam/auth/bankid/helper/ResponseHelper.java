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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseHelper {
    @JsonProperty
    private String ssn;
    @JsonProperty
    private String uid;
    @JsonProperty
    private String cn;
    @JsonProperty
    private String sn;
    @JsonProperty
    private String errorCode;

    public ResponseHelper(String ssn, String uid, String cn) {
        this.ssn = ssn;
        this.uid = uid;
        this.cn = cn;
        String split[] = cn.split(",", 2);
        if (split.length == 2) {
            this.sn = split[0];
        } else {
            this.sn = cn;   
        }
        this.errorCode = null;
    }

    public ResponseHelper(String uid, String cn) {
        this(null, uid, cn);
    }

    public ResponseHelper(String errorCode) {
        this.errorCode = errorCode;
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

    public boolean isError() {
        return errorCode != null;
    }

    public String getSsn() {
        return ssn;
    }

    public String getUid() {
        return uid;
    }

    public String getCn() {
        return cn;
    }

    public String getSn() {
        return sn;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
