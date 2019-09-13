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
package org.apache.syncope.client.cli.commands.info;

import java.util.Collection;
import org.apache.syncope.client.cli.commands.CommonsResultManager;

public class InfoResultManager extends CommonsResultManager {

    public void printVersion(final String version) {
        genericMessage("Syncope version: " + version);
    }

    public void printPwdResetAllowed(final boolean pwdResetAllowed) {
        genericMessage("Password reset allowed: " + pwdResetAllowed);
    }

    public void printPwdResetRequiringSecurityQuestions(final boolean pwdResetRequiringSecurityQuestions) {
        genericMessage("Password reset requiring security question: " + pwdResetRequiringSecurityQuestions);
    }

    public void printSelfRegistrationAllowed(final boolean selfRegAllowed) {
        genericMessage("Self registration allowed: " + selfRegAllowed);
    }

    public void printProvisioningManager(final String anyObjectProvisioningManager,
            final String getUserProvisioningManager,
            final String getGroupProvisioningManager) {
        genericMessage(
                "Any object provisioning manager class: " + anyObjectProvisioningManager,
                "User provisioning manager class: " + getUserProvisioningManager,
                "Group provisioning manager class: " + getGroupProvisioningManager);
    }

    public void printAccountRules(final Collection<String> rules) {
        rules.forEach(accountRule -> {
            genericMessage("Account rule: " + accountRule);
        });
    }

    public void printConnidLocations(final Collection<String> locations) {
        locations.forEach(location -> {
            genericMessage("ConnId location: " + location);
        });
    }

    public void printReconFilterBuilders(final Collection<String> reconFilterBuilders) {
        reconFilterBuilders.forEach(reconciliationFilterBuilder -> {
            genericMessage("Reconciliation filter builder: " + reconciliationFilterBuilder);
        });
    }

    public void printLogicActions(final Collection<String> actions) {
        actions.forEach(action -> {
            genericMessage("Logic action: " + action);
        });
    }

    public void printItemTransformers(final Collection<String> transformers) {
        transformers.forEach(tranformer -> {
            genericMessage("Mapping item tranformer: " + tranformer);
        });
    }

    public void printPasswordRules(final Collection<String> rules) {
        rules.forEach(rule -> {
            genericMessage("Password rule: " + rule);
        });
    }

    public void printCorrelationRules(final Collection<String> rules) {
        rules.forEach(rule -> {
            genericMessage("Correlation rule: " + rule);
        });
    }

    public void printPropagationActions(final Collection<String> actions) {
        actions.forEach(action -> {
            genericMessage("Propagation action: " + action);
        });
    }

    public void printPushActions(final Collection<String> actions) {
        actions.forEach(action -> {
            genericMessage("Push action: " + action);
        });
    }

    public void printPullActions(final Collection<String> actions) {
        actions.forEach(action -> {
            genericMessage("Sync action: " + action);
        });
    }

    public void printCorrelationActions(final Collection<String> actions) {
        actions.forEach(action -> {
            genericMessage("Push correlation rule: " + action);
        });
    }

    public void printReportletConfs(final Collection<String> reportletConfs) {
        reportletConfs.forEach(reportletConf -> {
            genericMessage("Reportlet conf: " + reportletConf);
        });
    }

    public void printJobs(final Collection<String> jobs) {
        jobs.forEach(job -> {
            genericMessage("Task job: " + job);
        });
    }

    public void printValidators(final Collection<String> validators) {
        validators.forEach(validator -> {
            genericMessage("Validator: " + validator);
        });
    }

    public void printPasswordGenerator(final String passwordGenerator) {
        genericMessage("Password generator class: " + passwordGenerator);
    }

    public void printVirtualAttributeCacheClass(final String virAttrCache) {
        genericMessage("Virtual attribute cache class: " + virAttrCache);
    }
}
