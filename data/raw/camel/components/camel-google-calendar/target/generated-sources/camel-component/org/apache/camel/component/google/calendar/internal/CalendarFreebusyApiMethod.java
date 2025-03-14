/*
 * Camel ApiMethod Enumeration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:12:02 CEST 2019
 */
package org.apache.camel.component.google.calendar.internal;

import java.lang.reflect.Method;
import java.util.List;

import com.google.api.services.calendar.Calendar.Freebusy;

import org.apache.camel.util.component.ApiMethod;
import org.apache.camel.util.component.ApiMethodArg;
import org.apache.camel.util.component.ApiMethodImpl;

import static org.apache.camel.util.component.ApiMethodArg.arg;

/**
 * Camel {@link ApiMethod} Enumeration for com.google.api.services.calendar.Calendar$Freebusy
 */
public enum CalendarFreebusyApiMethod implements ApiMethod {

    QUERY(
        com.google.api.services.calendar.Calendar.Freebusy.Query.class,
        "query",
        arg("content", com.google.api.services.calendar.model.FreeBusyRequest.class));
    

    private final ApiMethod apiMethod;

    private CalendarFreebusyApiMethod(Class<?> resultType, String name, ApiMethodArg... args) {
        this.apiMethod = new ApiMethodImpl(Freebusy.class, resultType, name, args);
    }

    @Override
    public String getName() { return apiMethod.getName(); }

    @Override
    public Class<?> getResultType() { return apiMethod.getResultType(); }

    @Override
    public List<String> getArgNames() { return apiMethod.getArgNames(); }

    @Override
    public List<Class<?>> getArgTypes() { return apiMethod.getArgTypes(); }

    @Override
    public Method getMethod() { return apiMethod.getMethod(); }
}
