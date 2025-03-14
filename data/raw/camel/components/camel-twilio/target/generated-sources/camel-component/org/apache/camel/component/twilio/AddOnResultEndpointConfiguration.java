/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:31:49 CEST 2019
 */
package org.apache.camel.component.twilio;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for com.twilio.rest.api.v2010.account.recording.AddOnResult
 */
@UriParams
public final class AddOnResultEndpointConfiguration extends TwilioConfiguration {

    @UriParam
    private String pathAccountSid;

    @UriParam
    private String pathReferenceSid;

    @UriParam
    private String pathSid;

    public String getPathAccountSid() {
        return pathAccountSid;
    }

    public void setPathAccountSid(String pathAccountSid) {
        this.pathAccountSid = pathAccountSid;
    }

    public String getPathReferenceSid() {
        return pathReferenceSid;
    }

    public void setPathReferenceSid(String pathReferenceSid) {
        this.pathReferenceSid = pathReferenceSid;
    }

    public String getPathSid() {
        return pathSid;
    }

    public void setPathSid(String pathSid) {
        this.pathSid = pathSid;
    }
}
