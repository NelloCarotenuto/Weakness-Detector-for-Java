/*
 * Camel ApiMethod Enumeration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:20:08 CEST 2019
 */
package org.apache.camel.component.linkedin.internal;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.camel.component.linkedin.api.GroupsResource;

import org.apache.camel.util.component.ApiMethod;
import org.apache.camel.util.component.ApiMethodArg;
import org.apache.camel.util.component.ApiMethodImpl;

import static org.apache.camel.util.component.ApiMethodArg.arg;

/**
 * Camel {@link ApiMethod} Enumeration for org.apache.camel.component.linkedin.api.GroupsResource
 */
public enum GroupsResourceApiMethod implements ApiMethod {

    ADDPOST(
        void.class,
        "addPost",
        arg("group_id", long.class),
        arg("post", org.apache.camel.component.linkedin.api.model.Post.class)),
    GETGROUP(
        org.apache.camel.component.linkedin.api.model.Group.class,
        "getGroup",
        arg("group_id", long.class),
        arg("fields", String.class)),
    GETPOSTS(
        org.apache.camel.component.linkedin.api.model.Posts.class,
        "getPosts",
        arg("group_id", long.class),
        arg("start", Long.class),
        arg("count", Long.class),
        arg("order", org.apache.camel.component.linkedin.api.model.Order.class),
        arg("category", org.apache.camel.component.linkedin.api.model.PostCategoryCode.class),
        arg("modified_since", Long.class),
        arg("fields", String.class));
    

    private final ApiMethod apiMethod;

    private GroupsResourceApiMethod(Class<?> resultType, String name, ApiMethodArg... args) {
        this.apiMethod = new ApiMethodImpl(GroupsResource.class, resultType, name, args);
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
