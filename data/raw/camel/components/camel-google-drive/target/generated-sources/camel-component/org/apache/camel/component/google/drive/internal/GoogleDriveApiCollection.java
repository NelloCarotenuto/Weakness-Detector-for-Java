/*
 * Camel ApiCollection generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:12:10 CEST 2019
 */
package org.apache.camel.component.google.drive.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.camel.component.google.drive.GoogleDriveConfiguration;
import org.apache.camel.component.google.drive.DriveAboutEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveAppsEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveChangesEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveChannelsEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveChildrenEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveCommentsEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveFilesEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveParentsEndpointConfiguration;
import org.apache.camel.component.google.drive.DrivePermissionsEndpointConfiguration;
import org.apache.camel.component.google.drive.DrivePropertiesEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveRealtimeEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveRepliesEndpointConfiguration;
import org.apache.camel.component.google.drive.DriveRevisionsEndpointConfiguration;

import org.apache.camel.util.component.ApiCollection;
import org.apache.camel.util.component.ApiMethod;
import org.apache.camel.util.component.ApiMethodHelper;

/**
 * Camel {@link ApiCollection} for GoogleDrive
 */
public final class GoogleDriveApiCollection extends ApiCollection<GoogleDriveApiName, GoogleDriveConfiguration> {

    private static GoogleDriveApiCollection collection;

    private GoogleDriveApiCollection() {
        final Map<String, String> aliases = new HashMap<String, String>();
        final Map<GoogleDriveApiName, ApiMethodHelper<? extends ApiMethod>> apiHelpers = new HashMap<>();
        final Map<Class<? extends ApiMethod>, GoogleDriveApiName> apiMethods = new HashMap<>();

        List<String> nullableArgs;

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_ABOUT, new ApiMethodHelper<DriveAboutApiMethod>(DriveAboutApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveAboutApiMethod.class, GoogleDriveApiName.DRIVE_ABOUT);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_APPS, new ApiMethodHelper<DriveAppsApiMethod>(DriveAppsApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveAppsApiMethod.class, GoogleDriveApiName.DRIVE_APPS);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_CHANGES, new ApiMethodHelper<DriveChangesApiMethod>(DriveChangesApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveChangesApiMethod.class, GoogleDriveApiName.DRIVE_CHANGES);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_CHANNELS, new ApiMethodHelper<DriveChannelsApiMethod>(DriveChannelsApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveChannelsApiMethod.class, GoogleDriveApiName.DRIVE_CHANNELS);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_CHILDREN, new ApiMethodHelper<DriveChildrenApiMethod>(DriveChildrenApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveChildrenApiMethod.class, GoogleDriveApiName.DRIVE_CHILDREN);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_COMMENTS, new ApiMethodHelper<DriveCommentsApiMethod>(DriveCommentsApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveCommentsApiMethod.class, GoogleDriveApiName.DRIVE_COMMENTS);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_FILES, new ApiMethodHelper<DriveFilesApiMethod>(DriveFilesApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveFilesApiMethod.class, GoogleDriveApiName.DRIVE_FILES);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_PARENTS, new ApiMethodHelper<DriveParentsApiMethod>(DriveParentsApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveParentsApiMethod.class, GoogleDriveApiName.DRIVE_PARENTS);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_PERMISSIONS, new ApiMethodHelper<DrivePermissionsApiMethod>(DrivePermissionsApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DrivePermissionsApiMethod.class, GoogleDriveApiName.DRIVE_PERMISSIONS);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_PROPERTIES, new ApiMethodHelper<DrivePropertiesApiMethod>(DrivePropertiesApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DrivePropertiesApiMethod.class, GoogleDriveApiName.DRIVE_PROPERTIES);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_REALTIME, new ApiMethodHelper<DriveRealtimeApiMethod>(DriveRealtimeApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveRealtimeApiMethod.class, GoogleDriveApiName.DRIVE_REALTIME);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_REPLIES, new ApiMethodHelper<DriveRepliesApiMethod>(DriveRepliesApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveRepliesApiMethod.class, GoogleDriveApiName.DRIVE_REPLIES);

        aliases.clear();
        nullableArgs = Arrays.asList();
        apiHelpers.put(GoogleDriveApiName.DRIVE_REVISIONS, new ApiMethodHelper<DriveRevisionsApiMethod>(DriveRevisionsApiMethod.class, aliases, nullableArgs));
        apiMethods.put(DriveRevisionsApiMethod.class, GoogleDriveApiName.DRIVE_REVISIONS);

        setApiHelpers(apiHelpers);
        setApiMethods(apiMethods);
    }

    public GoogleDriveConfiguration getEndpointConfiguration(GoogleDriveApiName apiName) {
        GoogleDriveConfiguration result = null;
        switch (apiName) {
        case DRIVE_ABOUT:
            result = new DriveAboutEndpointConfiguration();
            break;
        case DRIVE_APPS:
            result = new DriveAppsEndpointConfiguration();
            break;
        case DRIVE_CHANGES:
            result = new DriveChangesEndpointConfiguration();
            break;
        case DRIVE_CHANNELS:
            result = new DriveChannelsEndpointConfiguration();
            break;
        case DRIVE_CHILDREN:
            result = new DriveChildrenEndpointConfiguration();
            break;
        case DRIVE_COMMENTS:
            result = new DriveCommentsEndpointConfiguration();
            break;
        case DRIVE_FILES:
            result = new DriveFilesEndpointConfiguration();
            break;
        case DRIVE_PARENTS:
            result = new DriveParentsEndpointConfiguration();
            break;
        case DRIVE_PERMISSIONS:
            result = new DrivePermissionsEndpointConfiguration();
            break;
        case DRIVE_PROPERTIES:
            result = new DrivePropertiesEndpointConfiguration();
            break;
        case DRIVE_REALTIME:
            result = new DriveRealtimeEndpointConfiguration();
            break;
        case DRIVE_REPLIES:
            result = new DriveRepliesEndpointConfiguration();
            break;
        case DRIVE_REVISIONS:
            result = new DriveRevisionsEndpointConfiguration();
            break;
        }
        return result;
    }

    public static synchronized GoogleDriveApiCollection getCollection() {
        if (collection == null) {
            collection = new GoogleDriveApiCollection();
        }
        return collection;
    }
}
