/*
 * Camel ApiMethod Enumeration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:12:10 CEST 2019
 */
package org.apache.camel.component.google.drive.internal;

import java.lang.reflect.Method;
import java.util.List;

import com.google.api.services.drive.Drive.Revisions;

import org.apache.camel.util.component.ApiMethod;
import org.apache.camel.util.component.ApiMethodArg;
import org.apache.camel.util.component.ApiMethodImpl;

import static org.apache.camel.util.component.ApiMethodArg.arg;

/**
 * Camel {@link ApiMethod} Enumeration for com.google.api.services.drive.Drive$Revisions
 */
public enum DriveRevisionsApiMethod implements ApiMethod {

    DELETE(
        com.google.api.services.drive.Drive.Revisions.Delete.class,
        "delete",
        arg("fileId", String.class),
        arg("revisionId", String.class)),
    GET(
        com.google.api.services.drive.Drive.Revisions.Get.class,
        "get",
        arg("fileId", String.class),
        arg("revisionId", String.class)),
    LIST(
        com.google.api.services.drive.Drive.Revisions.List.class,
        "list",
        arg("fileId", String.class)),
    PATCH(
        com.google.api.services.drive.Drive.Revisions.Patch.class,
        "patch",
        arg("fileId", String.class),
        arg("revisionId", String.class),
        arg("content", com.google.api.services.drive.model.Revision.class)),
    UPDATE(
        com.google.api.services.drive.Drive.Revisions.Update.class,
        "update",
        arg("fileId", String.class),
        arg("revisionId", String.class),
        arg("content", com.google.api.services.drive.model.Revision.class));
    

    private final ApiMethod apiMethod;

    private DriveRevisionsApiMethod(Class<?> resultType, String name, ApiMethodArg... args) {
        this.apiMethod = new ApiMethodImpl(Revisions.class, resultType, name, args);
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
