/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.cli.commands.policy;

import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;

public class PolicyResultManager extends CommonsResultManager {

    public void printPolicies(final List<PolicyTO> policyTOs) {
        System.out.println("");
        policyTOs.forEach(policyTO -> {
            if (policyTO instanceof AccountPolicyTO) {
                printAccountPolicy((AccountPolicyTO) policyTO);
            } else if (policyTO instanceof PasswordPolicyTO) {
                printPasswordPolicy((PasswordPolicyTO) policyTO);
            } else if (policyTO instanceof PullPolicyTO) {
                printPullPolicy((PullPolicyTO) policyTO);
            }
        });
    }

    public void printPoliciesByType(final String policyTypeString, final List<PolicyTO> policyTOs) {
        System.out.println("");
        final PolicyType policyType = PolicyType.valueOf(policyTypeString);
        switch (policyType) {
            case ACCOUNT:
                policyTOs.forEach(policyTO -> {
                    printAccountPolicy((AccountPolicyTO) policyTO);
                });
                break;
            case PASSWORD:
                policyTOs.forEach(policyTO -> {
                    printPasswordPolicy((PasswordPolicyTO) policyTO);
                });
                break;
            case PUSH:
                policyTOs.forEach(policyTO -> {
                    System.out.println(policyTO);
                });
                break;
            case PULL:
                policyTOs.forEach(policyTO -> {
                    printPullPolicy((PullPolicyTO) policyTO);
                });
                break;
            default:
                break;
        }
    }

    public void printAccountPolicy(final AccountPolicyTO policyTO) {
        System.out.println(" > KEY: " + policyTO.getKey());
        System.out.println("    type: " + policyTO.getClass().getSimpleName());
        System.out.println("    description: " + policyTO.getDescription());
        System.out.println("    resources : " + policyTO.getUsedByResources().toString());
        System.out.println("    realms : " + policyTO.getUsedByRealms().toString());
        System.out.println("    max authentication attempts : " + policyTO.getMaxAuthenticationAttempts());
        System.out.println("    propagation suspension : " + policyTO.isPropagateSuspension());
        System.out.println("    RULES : ");
        System.out.println("       > class : " + policyTO.getRules());
        System.out.println("");
    }

    public void printPasswordPolicy(final PasswordPolicyTO policyTO) {
        System.out.println(" > KEY: " + policyTO.getKey());
        System.out.println("    type: " + policyTO.getClass().getSimpleName());
        System.out.println("    description: " + policyTO.getDescription());
        System.out.println("    resources : " + policyTO.getUsedByResources().toString());
        System.out.println("    realms : " + policyTO.getUsedByRealms().toString());
        System.out.println("    history lenght : " + policyTO.getHistoryLength());
        System.out.println("    allow null password : " + policyTO.isAllowNullPassword());
        System.out.println("    RULES : ");
        System.out.println("       > class : " + policyTO.getRules());
        System.out.println("");
    }

    public void printPullPolicy(final PullPolicyTO policyTO) {
        System.out.println(" > KEY: " + policyTO.getKey());
        System.out.println("    type: " + policyTO.getClass().getSimpleName());
        System.out.println("    description: " + policyTO.getDescription());
        System.out.println("    resources : " + policyTO.getUsedByResources().toString());
        System.out.println("    realms : " + policyTO.getUsedByRealms().toString());
        System.out.println("    conflict resolution action: "
                + policyTO.getConflictResolutionAction().name());
        System.out.println("    correlation rules : "
                + policyTO.getCorrelationRules().toString());
        System.out.println("");
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("policies details", details);
    }
}
