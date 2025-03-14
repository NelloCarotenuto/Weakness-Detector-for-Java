/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:05:39 CEST 2019
 */
package org.apache.camel.component.braintree;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for com.braintreegateway.WebhookNotificationGateway
 */
@UriParams
public final class WebhookNotificationGatewayEndpointConfiguration extends BraintreeConfiguration {

    @UriParam
    private String challenge;

    @UriParam
    private String payload;

    @UriParam
    private String signature;

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
