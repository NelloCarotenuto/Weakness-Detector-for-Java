/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:12:10 CEST 2019
 */
package org.apache.camel.component.google.drive;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for com.google.api.services.drive.Drive$Parents
 */
@UriParams
public final class DriveParentsEndpointConfiguration extends GoogleDriveConfiguration {

    @UriParam
    private com.google.api.services.drive.model.ParentReference content;

    @UriParam
    private String fileId;

    @UriParam
    private String parentId;

    public com.google.api.services.drive.model.ParentReference getContent() {
        return content;
    }

    public void setContent(com.google.api.services.drive.model.ParentReference content) {
        this.content = content;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
