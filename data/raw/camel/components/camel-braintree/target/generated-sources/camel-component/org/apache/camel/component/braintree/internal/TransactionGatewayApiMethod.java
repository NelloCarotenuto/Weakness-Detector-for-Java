/*
 * Camel ApiMethod Enumeration generated by camel-api-component-maven-plugin
 * Generated on: Wed Sep 11 15:05:39 CEST 2019
 */
package org.apache.camel.component.braintree.internal;

import java.lang.reflect.Method;
import java.util.List;

import com.braintreegateway.TransactionGateway;

import org.apache.camel.util.component.ApiMethod;
import org.apache.camel.util.component.ApiMethodArg;
import org.apache.camel.util.component.ApiMethodImpl;

import static org.apache.camel.util.component.ApiMethodArg.arg;

/**
 * Camel {@link ApiMethod} Enumeration for com.braintreegateway.TransactionGateway
 */
public enum TransactionGatewayApiMethod implements ApiMethod {

    CANCELRELEASE(
        com.braintreegateway.Result.class,
        "cancelRelease",
        arg("id", String.class)),
    CLONETRANSACTION(
        com.braintreegateway.Result.class,
        "cloneTransaction",
        arg("id", String.class),
        arg("cloneRequest", com.braintreegateway.TransactionCloneRequest.class)),
    CREDIT(
        com.braintreegateway.Result.class,
        "credit",
        arg("request", com.braintreegateway.TransactionRequest.class)),
    FIND(
        com.braintreegateway.Transaction.class,
        "find",
        arg("id", String.class)),
    HOLDINESCROW(
        com.braintreegateway.Result.class,
        "holdInEscrow",
        arg("id", String.class)),
    REFUND(
        com.braintreegateway.Result.class,
        "refund",
        arg("id", String.class)),
    REFUND_1(
        com.braintreegateway.Result.class,
        "refund",
        arg("id", String.class),
        arg("amount", java.math.BigDecimal.class)),
    RELEASEFROMESCROW(
        com.braintreegateway.Result.class,
        "releaseFromEscrow",
        arg("id", String.class)),
    SALE(
        com.braintreegateway.Result.class,
        "sale",
        arg("request", com.braintreegateway.TransactionRequest.class)),
    SEARCH(
        com.braintreegateway.ResourceCollection.class,
        "search",
        arg("query", com.braintreegateway.TransactionSearchRequest.class)),
    SUBMITFORPARTIALSETTLEMENT(
        com.braintreegateway.Result.class,
        "submitForPartialSettlement",
        arg("id", String.class),
        arg("amount", java.math.BigDecimal.class)),
    SUBMITFORPARTIALSETTLEMENT_1(
        com.braintreegateway.Result.class,
        "submitForPartialSettlement",
        arg("id", String.class),
        arg("request", com.braintreegateway.TransactionRequest.class)),
    SUBMITFORSETTLEMENT(
        com.braintreegateway.Result.class,
        "submitForSettlement",
        arg("id", String.class)),
    SUBMITFORSETTLEMENT_1(
        com.braintreegateway.Result.class,
        "submitForSettlement",
        arg("id", String.class),
        arg("amount", java.math.BigDecimal.class)),
    SUBMITFORSETTLEMENT_2(
        com.braintreegateway.Result.class,
        "submitForSettlement",
        arg("id", String.class),
        arg("request", com.braintreegateway.TransactionRequest.class)),
    UPDATEDETAILS(
        com.braintreegateway.Result.class,
        "updateDetails",
        arg("id", String.class),
        arg("request", com.braintreegateway.TransactionRequest.class)),
    VOIDTRANSACTION(
        com.braintreegateway.Result.class,
        "voidTransaction",
        arg("id", String.class));
    

    private final ApiMethod apiMethod;

    private TransactionGatewayApiMethod(Class<?> resultType, String name, ApiMethodArg... args) {
        this.apiMethod = new ApiMethodImpl(TransactionGateway.class, resultType, name, args);
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
