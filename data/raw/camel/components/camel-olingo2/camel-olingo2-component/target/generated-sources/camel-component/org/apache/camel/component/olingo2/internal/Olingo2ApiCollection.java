/*
 * Camel ApiCollection generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:23:04 CEST 2019
 */
package org.apache.camel.component.olingo2.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.camel.component.olingo2.Olingo2Configuration;
import org.apache.camel.component.olingo2.Olingo2AppEndpointConfiguration;

import org.apache.camel.util.component.ApiCollection;
import org.apache.camel.util.component.ApiMethod;
import org.apache.camel.util.component.ApiMethodHelper;

/**
 * Camel {@link ApiCollection} for Olingo2
 */
public final class Olingo2ApiCollection extends ApiCollection<Olingo2ApiName, Olingo2Configuration> {

    private static Olingo2ApiCollection collection;

    private Olingo2ApiCollection() {
        final Map<String, String> aliases = new HashMap<String, String>();
        final Map<Olingo2ApiName, ApiMethodHelper<? extends ApiMethod>> apiHelpers = new HashMap<>();
        final Map<Class<? extends ApiMethod>, Olingo2ApiName> apiMethods = new HashMap<>();

        List<String> nullableArgs;

        aliases.clear();
        nullableArgs = Arrays.asList("queryParams", "endpointHttpHeaders");
        apiHelpers.put(Olingo2ApiName.DEFAULT, new ApiMethodHelper<Olingo2AppApiMethod>(Olingo2AppApiMethod.class, aliases, nullableArgs));
        apiMethods.put(Olingo2AppApiMethod.class, Olingo2ApiName.DEFAULT);

        setApiHelpers(apiHelpers);
        setApiMethods(apiMethods);
    }

    public Olingo2Configuration getEndpointConfiguration(Olingo2ApiName apiName) {
        Olingo2Configuration result = null;
        switch (apiName) {
        case DEFAULT:
            result = new Olingo2AppEndpointConfiguration();
            break;
        }
        return result;
    }

    public static synchronized Olingo2ApiCollection getCollection() {
        if (collection == null) {
            collection = new Olingo2ApiCollection();
        }
        return collection;
    }
}
