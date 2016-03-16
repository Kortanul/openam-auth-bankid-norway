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

    @JsonProperty("marchantName")
    String marchantName;

    @JsonProperty("marchantWebAddress")
    String marchantWebAddress;

    @JsonProperty("marchantURL")
    String marchantURL;

    @JsonProperty("marchantFeDomain")
    String marchantFeDomain;

    @JsonProperty("marchantFeAncestors")
    String marchantFeAncestors;

    @JsonProperty("marchantKeystore")
    String marchantKeystore;

    @JsonIgnore
    String marchantKeystorePassword;

    @JsonProperty("marchantGrantedPolicies")
    String marchantGrantedPolicies;

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

    public String getMarchantName() {
        return marchantName;
    }

    public void setMarchantName(String marchantName) {
        this.marchantName = marchantName;
    }

    public String getMarchantWebAddress() {
        return marchantWebAddress;
    }

    public void setMarchantWebAddress(String marchantWebAddress) {
        this.marchantWebAddress = marchantWebAddress;
    }

    public String getMarchantURL() {
        return marchantURL;
    }

    public void setMarchantURL(String marchantURL) {
        this.marchantURL = marchantURL;
    }

    public String getMarchantFeDomain() {
        return marchantFeDomain;
    }

    public void setMarchantFeDomain(String marchantFeDomain) {
        this.marchantFeDomain = marchantFeDomain;
    }

    public String getMarchantFeAncestors() {
        return marchantFeAncestors;
    }

    public void setMarchantFeAncestors(String marchantFeAncestors) {
        this.marchantFeAncestors = marchantFeAncestors;
    }

    public String getMarchantKeystore() {
        return marchantKeystore;
    }

    public void setMarchantKeystore(String marchantKeystore) {
        this.marchantKeystore = marchantKeystore;
    }

    public String getMarchantKeystorePassword() {
        return marchantKeystorePassword;
    }

    public void setMarchantKeystorePassword(String marchantKeystorePassword) {
        this.marchantKeystorePassword = marchantKeystorePassword;
    }

    public String getMarchantGrantedPolicies() {
        return marchantGrantedPolicies;
    }

    public void setMarchantGrantedPolicies(String marchantGrantedPolicies) {
        this.marchantGrantedPolicies = marchantGrantedPolicies;
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
