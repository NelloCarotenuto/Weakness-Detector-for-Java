/*
 * Camel ApiName Enumeration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:12:17 CEST 2019
 */
package org.apache.camel.component.google.mail.internal;

import org.apache.camel.util.component.ApiName;

/**
 * Camel {@link ApiName} Enumeration for Component GoogleMail
 */
public enum GoogleMailApiName implements ApiName {

    THREADS("threads"),
    MESSAGES("messages"),
    ATTACHMENTS("attachments"),
    LABELS("labels"),
    HISTORY("history"),
    DRAFTS("drafts"),
    USERS("users");

    private static final GoogleMailApiName[] VALUES = values();
    
    private final String name;

    private GoogleMailApiName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static GoogleMailApiName fromValue(String value) throws IllegalArgumentException {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].name.equals(value)) {
                return VALUES[i];
            }
        }
        throw new IllegalArgumentException("Invalid value " + value);
    }
}
