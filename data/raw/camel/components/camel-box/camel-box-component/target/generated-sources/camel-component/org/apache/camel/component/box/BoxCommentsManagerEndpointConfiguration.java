/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:05:30 CEST 2019
 */
package org.apache.camel.component.box;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for org.apache.camel.component.box.api.BoxCommentsManager
 */
@UriParams
public final class BoxCommentsManagerEndpointConfiguration extends BoxConfiguration {

    @UriParam
    private String commentId;

    @UriParam
    private String fileId;

    @UriParam
    private String message;

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
