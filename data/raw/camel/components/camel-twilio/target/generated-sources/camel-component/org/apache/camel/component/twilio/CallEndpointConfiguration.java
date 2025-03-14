/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:31:49 CEST 2019
 */
package org.apache.camel.component.twilio;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for com.twilio.rest.api.v2010.account.Call
 */
@UriParams
public final class CallEndpointConfiguration extends TwilioConfiguration {

    @UriParam
    private String applicationSid;

    @UriParam
    private com.twilio.type.PhoneNumber from;

    @UriParam
    private String pathAccountSid;

    @UriParam
    private String pathSid;

    @UriParam
    private com.twilio.type.Endpoint to;

    @UriParam
    private java.net.URI url;

    public String getApplicationSid() {
        return applicationSid;
    }

    public void setApplicationSid(String applicationSid) {
        this.applicationSid = applicationSid;
    }

    public com.twilio.type.PhoneNumber getFrom() {
        return from;
    }

    public void setFrom(com.twilio.type.PhoneNumber from) {
        this.from = from;
    }

    public String getPathAccountSid() {
        return pathAccountSid;
    }

    public void setPathAccountSid(String pathAccountSid) {
        this.pathAccountSid = pathAccountSid;
    }

    public String getPathSid() {
        return pathSid;
    }

    public void setPathSid(String pathSid) {
        this.pathSid = pathSid;
    }

    public com.twilio.type.Endpoint getTo() {
        return to;
    }

    public void setTo(com.twilio.type.Endpoint to) {
        this.to = to;
    }

    public java.net.URI getUrl() {
        return url;
    }

    public void setUrl(java.net.URI url) {
        this.url = url;
    }
}
