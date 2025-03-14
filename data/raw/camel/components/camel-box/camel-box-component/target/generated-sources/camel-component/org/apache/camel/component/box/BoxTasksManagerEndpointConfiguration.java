/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:05:31 CEST 2019
 */
package org.apache.camel.component.box;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for org.apache.camel.component.box.api.BoxTasksManager
 */
@UriParams
public final class BoxTasksManagerEndpointConfiguration extends BoxConfiguration {

    @UriParam
    private com.box.sdk.BoxTask.Action action;

    @UriParam
    private com.box.sdk.BoxUser assignTo;

    @UriParam
    private java.util.Date dueAt;

    @UriParam
    private String fileId;

    @UriParam
    private com.box.sdk.BoxTask.Info info;

    @UriParam
    private String message;

    @UriParam
    private String taskAssignmentId;

    @UriParam
    private String taskId;

    public com.box.sdk.BoxTask.Action getAction() {
        return action;
    }

    public void setAction(com.box.sdk.BoxTask.Action action) {
        this.action = action;
    }

    public com.box.sdk.BoxUser getAssignTo() {
        return assignTo;
    }

    public void setAssignTo(com.box.sdk.BoxUser assignTo) {
        this.assignTo = assignTo;
    }

    public java.util.Date getDueAt() {
        return dueAt;
    }

    public void setDueAt(java.util.Date dueAt) {
        this.dueAt = dueAt;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public com.box.sdk.BoxTask.Info getInfo() {
        return info;
    }

    public void setInfo(com.box.sdk.BoxTask.Info info) {
        this.info = info;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTaskAssignmentId() {
        return taskAssignmentId;
    }

    public void setTaskAssignmentId(String taskAssignmentId) {
        this.taskAssignmentId = taskAssignmentId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
