/*
 * Camel EndpointConfiguration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:20:08 CEST 2019
 */
package org.apache.camel.component.linkedin;

import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

/**
 * Camel EndpointConfiguration for org.apache.camel.component.linkedin.api.PostsResource
 */
@UriParams
public final class PostsResourceEndpointConfiguration extends LinkedInConfiguration {

    @UriParam
    private org.apache.camel.component.linkedin.api.model.Comment comment;

    @UriParam
    private Long count;

    @UriParam
    private String fields;

    @UriParam
    private org.apache.camel.component.linkedin.api.model.IsFollowing isfollowing;

    @UriParam
    private org.apache.camel.component.linkedin.api.model.IsLiked isliked;

    @UriParam
    private String post_id;

    @UriParam
    private org.apache.camel.component.linkedin.api.model.PostCategoryCode postcategorycode;

    @UriParam
    private Long start;

    public org.apache.camel.component.linkedin.api.model.Comment getComment() {
        return comment;
    }

    public void setComment(org.apache.camel.component.linkedin.api.model.Comment comment) {
        this.comment = comment;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }

    public org.apache.camel.component.linkedin.api.model.IsFollowing getIsfollowing() {
        return isfollowing;
    }

    public void setIsfollowing(org.apache.camel.component.linkedin.api.model.IsFollowing isfollowing) {
        this.isfollowing = isfollowing;
    }

    public org.apache.camel.component.linkedin.api.model.IsLiked getIsliked() {
        return isliked;
    }

    public void setIsliked(org.apache.camel.component.linkedin.api.model.IsLiked isliked) {
        this.isliked = isliked;
    }

    public String getPost_id() {
        return post_id;
    }

    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public org.apache.camel.component.linkedin.api.model.PostCategoryCode getPostcategorycode() {
        return postcategorycode;
    }

    public void setPostcategorycode(org.apache.camel.component.linkedin.api.model.PostCategoryCode postcategorycode) {
        this.postcategorycode = postcategorycode;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }
}
