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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class BankIDConfiguration {
    @JsonProperty("clientType")
    String clientType;

    @JsonProperty("merchantName")
    String merchantName;

    @JsonProperty("merchantWebAddress")
    String merchantWebAddress;

    @JsonProperty("merchantURL")
    String merchantURL;

    @JsonProperty("merchantFeDomain")
    String merchantFeDomain;

    @JsonProperty("merchantFeAncestors")
    String merchantFeAncestors;

    @JsonProperty("merchantKeystore")
    String merchantKeystore;

    @JsonIgnore
    String merchantKeystorePassword;

    @JsonProperty("merchantGrantedPolicies")
    String merchantGrantedPolicies;

    @JsonProperty("retrieveSSN")
    boolean retrieveSSN;

    @JsonProperty("propsMappings")
    Map<String, String> propsMappings;

    @JsonProperty("nextURL")
    String nextURL;

    @JsonProperty("timeout")
    String timeout;

    @JsonProperty("withCredentials")
    String withCredentials;

    @JsonProperty("sessionTimeout")
    String sessionTimeout;

    public String getClientType() {
        return clientType;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantWebAddress() {
        return merchantWebAddress;
    }

    public void setMerchantWebAddress(String merchantWebAddress) {
        this.merchantWebAddress = merchantWebAddress;
    }

    public String getMerchantURL() {
        return merchantURL;
    }

    public void setMerchantURL(String merchantURL) {
        this.merchantURL = merchantURL;
    }

    public String getMerchantFeDomain() {
        return merchantFeDomain;
    }

    public void setMerchantFeDomain(String merchantFeDomain) {
        this.merchantFeDomain = merchantFeDomain;
    }

    public String getMerchantFeAncestors() {
        return merchantFeAncestors;
    }

    public void setMerchantFeAncestors(String merchantFeAncestors) {
        this.merchantFeAncestors = merchantFeAncestors;
    }

    public String getMerchantKeystore() {
        return merchantKeystore;
    }

    public void setMerchantKeystore(String merchantKeystore) {
        this.merchantKeystore = merchantKeystore;
    }

    public String getMerchantKeystorePassword() {
        return merchantKeystorePassword;
    }

    public void setMerchantKeystorePassword(String merchantKeystorePassword) {
        this.merchantKeystorePassword = merchantKeystorePassword;
    }

    public String getMerchantGrantedPolicies() {
        return merchantGrantedPolicies;
    }

    public void setMerchantGrantedPolicies(String merchantGrantedPolicies) {
        this.merchantGrantedPolicies = merchantGrantedPolicies;
    }

    public boolean isRetrieveSSN() {
        return retrieveSSN;
    }

    public void setRetrieveSSN(boolean retrieveSSN) {
        this.retrieveSSN = retrieveSSN;
    }

    public Map<String, String> getPropsMappings() {
        return propsMappings;
    }

    public void setPropsMappings(Map<String, String> propsMappings) {
        this.propsMappings = propsMappings;
    }

    public String getNextURL() {
        return nextURL;
    }

    public void setNextURL(String nextURL) {
        this.nextURL = nextURL;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public String getWithCredentials() {
        return withCredentials;
    }

    public void setWithCredentials(String withCredentials) {
        this.withCredentials = withCredentials;
    }

    public String getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(String sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
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
}
