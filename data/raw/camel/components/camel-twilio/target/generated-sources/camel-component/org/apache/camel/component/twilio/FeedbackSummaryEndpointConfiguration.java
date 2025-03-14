/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:31:49 CEST 2019
 */
package org.apache.camel.component.twilio;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for com.twilio.rest.api.v2010.account.call.FeedbackSummary
 */
@UriParams
public final class FeedbackSummaryEndpointConfiguration extends TwilioConfiguration {

    @UriParam
    private org.joda.time.LocalDate endDate;

    @UriParam
    private String pathAccountSid;

    @UriParam
    private String pathSid;

    @UriParam
    private org.joda.time.LocalDate startDate;

    public org.joda.time.LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(org.joda.time.LocalDate endDate) {
        this.endDate = endDate;
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

    public org.joda.time.LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(org.joda.time.LocalDate startDate) {
        this.startDate = startDate;
    }
}
