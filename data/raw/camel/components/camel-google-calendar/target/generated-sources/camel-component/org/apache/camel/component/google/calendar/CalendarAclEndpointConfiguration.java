/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:12:02 CEST 2019
 */
package org.apache.camel.component.google.calendar;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for com.google.api.services.calendar.Calendar$Acl
 */
@UriParams
public final class CalendarAclEndpointConfiguration extends GoogleCalendarConfiguration {

    @UriParam
    private String calendarId;

    @UriParam
    private com.google.api.services.calendar.model.AclRule content;

    @UriParam
    private com.google.api.services.calendar.model.Channel contentChannel;

    @UriParam
    private String ruleId;

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public com.google.api.services.calendar.model.AclRule getContent() {
        return content;
    }

    public void setContent(com.google.api.services.calendar.model.AclRule content) {
        this.content = content;
    }

    public com.google.api.services.calendar.model.Channel getContentChannel() {
        return contentChannel;
    }

    public void setContentChannel(com.google.api.services.calendar.model.Channel contentChannel) {
        this.contentChannel = contentChannel;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
}
