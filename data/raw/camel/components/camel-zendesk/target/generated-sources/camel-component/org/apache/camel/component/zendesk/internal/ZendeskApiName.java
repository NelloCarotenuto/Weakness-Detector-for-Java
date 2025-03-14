/*
 * Camel ApiName Enumeration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:33:12 CEST 2019
 */
package org.apache.camel.component.zendesk.internal;

import org.apache.camel.util.component.ApiName;

/**
 * Camel {@link ApiName} Enumeration for Component Zendesk
 */
public enum ZendeskApiName implements ApiName {

    DEFAULT("");

    private static final ZendeskApiName[] VALUES = values();
    
    private final String name;

    private ZendeskApiName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static ZendeskApiName fromValue(String value) throws IllegalArgumentException {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].name.equals(value)) {
                return VALUES[i];
            }
        }
        throw new IllegalArgumentException("Invalid value " + value);
    }
}
