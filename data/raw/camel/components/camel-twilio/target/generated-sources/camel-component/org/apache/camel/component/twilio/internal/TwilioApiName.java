/*
 * Camel ApiName Enumeration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:31:49 CEST 2019
 */
package org.apache.camel.component.twilio.internal;

import org.apache.camel.util.component.ApiName;

/**
 * Camel {@link ApiName} Enumeration for Component Twilio
 */
public enum TwilioApiName implements ApiName {

    ACCOUNT("account"),
    ADDRESS("address"),
    APPLICATION("application"),
    AVAILABLE_PHONE_NUMBER_COUNTRY("available-phone-number-country"),
    CALL("call"),
    CONFERENCE("conference"),
    CONNECT_APP("connect-app"),
    INCOMING_PHONE_NUMBER("incoming-phone-number"),
    KEY("key"),
    MESSAGE("message"),
    NEW_KEY("new-key"),
    NEW_SIGNING_KEY("new-signing-key"),
    NOTIFICATION("notification"),
    OUTGOING_CALLER_ID("outgoing-caller-id"),
    QUEUE("queue"),
    RECORDING("recording"),
    SHORT_CODE("short-code"),
    SIGNING_KEY("signing-key"),
    TOKEN("token"),
    TRANSCRIPTION("transcription"),
    VALIDATION_REQUEST("validation-request"),
    ADDRESS_DEPENDENT_PHONE_NUMBER("address-dependent-phone-number"),
    AVAILABLE_PHONE_NUMBER_COUNTRY_LOCAL("available-phone-number-country-local"),
    AVAILABLE_PHONE_NUMBER_COUNTRY_MOBILE("available-phone-number-country-mobile"),
    AVAILABLE_PHONE_NUMBER_COUNTRY_TOLL_FREE("available-phone-number-country-toll-free"),
    CALL_FEEDBACK("call-feedback"),
    CALL_FEEDBACK_SUMMARY("call-feedback-summary"),
    CALL_NOTIFICATION("call-notification"),
    CALL_RECORDING("call-recording"),
    CONFERENCE_PARTICIPANT("conference-participant"),
    INCOMING_PHONE_NUMBER_LOCAL("incoming-phone-number-local"),
    INCOMING_PHONE_NUMBER_MOBILE("incoming-phone-number-mobile"),
    INCOMING_PHONE_NUMBER_TOLL_FREE("incoming-phone-number-toll-free"),
    MESSAGE_FEEDBACK("message-feedback"),
    MESSAGE_MEDIA("message-media"),
    QUEUE_MEMBER("queue-member"),
    RECORDING_ADD_ON_RESULT("recording-add-on-result"),
    RECORDING_TRANSCRIPTION("recording-transcription"),
    RECORDING_ADD_ON_RESULT_PAYLOAD("recording-add-on-result-payload"),
    SIP_CREDENTIAL_LIST("sip-credential-list"),
    SIP_DOMAIN("sip-domain"),
    SIP_IP_ACCESS_CONTROL_LIST("sip-ip-access-control-list"),
    SIP_CREDENTIAL_LIST_CREDENTIAL("sip-credential-list-credential"),
    SIP_DOMAIN_CREDENTIAL_LIST_MAPPING("sip-domain-credential-list-mapping"),
    SIP_DOMAIN_IP_ACCESS_CONTROL_LIST_MAPPING("sip-domain-ip-access-control-list-mapping"),
    SIP_IP_ACCESS_CONTROL_LIST_IP_ADDRESS("sip-ip-access-control-list-ip-address"),
    USAGE_RECORD("usage-record"),
    USAGE_TRIGGER("usage-trigger"),
    USAGE_RECORD_ALL_TIME("usage-record-all-time"),
    USAGE_RECORD_DAILY("usage-record-daily"),
    USAGE_RECORD_LAST_MONTH("usage-record-last-month"),
    USAGE_RECORD_MONTHLY("usage-record-monthly"),
    USAGE_RECORD_THIS_MONTH("usage-record-this-month"),
    USAGE_RECORD_TODAY("usage-record-today"),
    USAGE_RECORD_YEARLY("usage-record-yearly"),
    USAGE_RECORD_YESTERDAY("usage-record-yesterday");

    private static final TwilioApiName[] VALUES = values();
    
    private final String name;

    private TwilioApiName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static TwilioApiName fromValue(String value) throws IllegalArgumentException {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].name.equals(value)) {
                return VALUES[i];
            }
        }
        throw new IllegalArgumentException("Invalid value " + value);
    }
}
