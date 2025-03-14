/*
 * Camel ApiMethod Enumeration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:20:08 CEST 2019
 */
package org.apache.camel.component.linkedin.internal;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.camel.component.linkedin.api.JobsResource;

import org.apache.camel.util.component.ApiMethod;
import org.apache.camel.util.component.ApiMethodArg;
import org.apache.camel.util.component.ApiMethodImpl;

import static org.apache.camel.util.component.ApiMethodArg.arg;

/**
 * Camel {@link ApiMethod} Enumeration for org.apache.camel.component.linkedin.api.JobsResource
 */
public enum JobsResourceApiMethod implements ApiMethod {

    ADDJOB(
        void.class,
        "addJob",
        arg("job", org.apache.camel.component.linkedin.api.model.Job.class)),
    EDITJOB(
        void.class,
        "editJob",
        arg("partner_job_id", long.class),
        arg("job", org.apache.camel.component.linkedin.api.model.Job.class)),
    GETJOB(
        org.apache.camel.component.linkedin.api.model.Job.class,
        "getJob",
        arg("job_id", long.class),
        arg("fields", String.class)),
    REMOVEJOB(
        void.class,
        "removeJob",
        arg("partner_job_id", long.class));
    

    private final ApiMethod apiMethod;

    private JobsResourceApiMethod(Class<?> resultType, String name, ApiMethodArg... args) {
        this.apiMethod = new ApiMethodImpl(JobsResource.class, resultType, name, args);
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
