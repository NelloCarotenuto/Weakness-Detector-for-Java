/*
 * Camel Api Route test generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:12:17 CEST 2019
 */
package org.apache.camel.component.google.mail;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.component.google.mail.internal.GoogleMailApiCollection;
import org.apache.camel.component.google.mail.internal.GmailUsersDraftsApiMethod;

/**
 * Test class for {@link com.google.api.services.gmail.Gmail$Users$Drafts} APIs.
 * TODO Move the file to src/test/java, populate parameter values, and remove @Ignore annotations.
 * The class source won't be generated again if the generator MOJO finds it under src/test/java.
 */
public class GmailUsersDraftsIntegrationTest extends AbstractGoogleMailTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(GmailUsersDraftsIntegrationTest.class);
    private static final String PATH_PREFIX = GoogleMailApiCollection.getCollection().getApiName(GmailUsersDraftsApiMethod.class).getName();

    // TODO provide parameter values for create
    @Ignore
    @Test
    public void testCreate() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", null);
        // parameter type is com.google.api.services.gmail.model.Draft
        headers.put("CamelGoogleMail.content", null);

        final com.google.api.services.gmail.Gmail.Users.Drafts.Create result = requestBodyAndHeaders("direct://CREATE", null, headers);

        assertNotNull("create result", result);
        LOG.debug("create: " + result);
    }

    // TODO provide parameter values for create
    @Ignore
    @Test
    public void testCreate_1() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", null);
        // parameter type is com.google.api.services.gmail.model.Draft
        headers.put("CamelGoogleMail.content", null);
        // parameter type is com.google.api.client.http.AbstractInputStreamContent
        headers.put("CamelGoogleMail.mediaContent", null);

        final com.google.api.services.gmail.Gmail.Users.Drafts.Create result = requestBodyAndHeaders("direct://CREATE_1", null, headers);

        assertNotNull("create result", result);
        LOG.debug("create: " + result);
    }

    // TODO provide parameter values for delete
    @Ignore
    @Test
    public void testDelete() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", null);
        // parameter type is String
        headers.put("CamelGoogleMail.id", null);

        final com.google.api.services.gmail.Gmail.Users.Drafts.Delete result = requestBodyAndHeaders("direct://DELETE", null, headers);

        assertNotNull("delete result", result);
        LOG.debug("delete: " + result);
    }

    // TODO provide parameter values for get
    @Ignore
    @Test
    public void testGet() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", null);
        // parameter type is String
        headers.put("CamelGoogleMail.id", null);

        final com.google.api.services.gmail.Gmail.Users.Drafts.Get result = requestBodyAndHeaders("direct://GET", null, headers);

        assertNotNull("get result", result);
        LOG.debug("get: " + result);
    }

    // TODO provide parameter values for list
    @Ignore
    @Test
    public void testList() throws Exception {
        // using String message body for single parameter "userId"
        final com.google.api.services.gmail.Gmail.Users.Drafts.List result = requestBody("direct://LIST", null);

        assertNotNull("list result", result);
        LOG.debug("list: " + result);
    }

    // TODO provide parameter values for send
    @Ignore
    @Test
    public void testSend() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", null);
        // parameter type is com.google.api.services.gmail.model.Draft
        headers.put("CamelGoogleMail.content", null);

        final com.google.api.services.gmail.Gmail.Users.Drafts.Send result = requestBodyAndHeaders("direct://SEND", null, headers);

        assertNotNull("send result", result);
        LOG.debug("send: " + result);
    }

    // TODO provide parameter values for send
    @Ignore
    @Test
    public void testSend_1() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", null);
        // parameter type is com.google.api.services.gmail.model.Draft
        headers.put("CamelGoogleMail.content", null);
        // parameter type is com.google.api.client.http.AbstractInputStreamContent
        headers.put("CamelGoogleMail.mediaContent", null);

        final com.google.api.services.gmail.Gmail.Users.Drafts.Send result = requestBodyAndHeaders("direct://SEND_1", null, headers);

        assertNotNull("send result", result);
        LOG.debug("send: " + result);
    }

    // TODO provide parameter values for update
    @Ignore
    @Test
    public void testUpdate() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", null);
        // parameter type is String
        headers.put("CamelGoogleMail.id", null);
        // parameter type is com.google.api.services.gmail.model.Draft
        headers.put("CamelGoogleMail.content", null);

        final com.google.api.services.gmail.Gmail.Users.Drafts.Update result = requestBodyAndHeaders("direct://UPDATE", null, headers);

        assertNotNull("update result", result);
        LOG.debug("update: " + result);
    }

    // TODO provide parameter values for update
    @Ignore
    @Test
    public void testUpdate_1() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelGoogleMail.userId", null);
        // parameter type is String
        headers.put("CamelGoogleMail.id", null);
        // parameter type is com.google.api.services.gmail.model.Draft
        headers.put("CamelGoogleMail.content", null);
        // parameter type is com.google.api.client.http.AbstractInputStreamContent
        headers.put("CamelGoogleMail.mediaContent", null);

        final com.google.api.services.gmail.Gmail.Users.Drafts.Update result = requestBodyAndHeaders("direct://UPDATE_1", null, headers);

        assertNotNull("update result", result);
        LOG.debug("update: " + result);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                // test route for create
                from("direct://CREATE")
                    .to("google-mail://" + PATH_PREFIX + "/create");

                // test route for create
                from("direct://CREATE_1")
                    .to("google-mail://" + PATH_PREFIX + "/create");

                // test route for delete
                from("direct://DELETE")
                    .to("google-mail://" + PATH_PREFIX + "/delete");

                // test route for get
                from("direct://GET")
                    .to("google-mail://" + PATH_PREFIX + "/get");

                // test route for list
                from("direct://LIST")
                    .to("google-mail://" + PATH_PREFIX + "/list?inBody=userId");

                // test route for send
                from("direct://SEND")
                    .to("google-mail://" + PATH_PREFIX + "/send");

                // test route for send
                from("direct://SEND_1")
                    .to("google-mail://" + PATH_PREFIX + "/send");

                // test route for update
                from("direct://UPDATE")
                    .to("google-mail://" + PATH_PREFIX + "/update");

                // test route for update
                from("direct://UPDATE_1")
                    .to("google-mail://" + PATH_PREFIX + "/update");

            }
        };
    }
}
