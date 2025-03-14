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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.core.flowable.api.BpmnProcessManager;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.flowable.api.UserRequestHandler;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserRequestLogic extends AbstractTransactionalLogic<EntityTO> {

    @Autowired
    protected BpmnProcessManager bpmnProcessManager;

    @Autowired
    protected UserRequestHandler userRequestHandler;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected UserDataBinder binder;

    @Autowired
    protected UserDAO userDAO;

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Pair<Integer, List<UserRequest>> list(final String userKey,
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        if (userKey == null) {
            securityChecks(null,
                    FlowableEntitlement.USER_REQUEST_LIST,
                    "Listing user requests not allowed");
        } else {
            User user = userDAO.find(userKey);
            if (user == null) {
                throw new NotFoundException("User " + userKey);
            }

            securityChecks(user.getUsername(),
                    FlowableEntitlement.USER_REQUEST_LIST,
                    "Listing requests for user" + user.getUsername() + " not allowed");
        }

        return userRequestHandler.getUserRequests(userKey, page, size, orderByClauses);
    }

    protected UserRequest doStart(final String bpmnProcess, final User user) {
        // check if BPMN process exists
        bpmnProcessManager.exportProcess(bpmnProcess, BpmnProcessFormat.XML, new NullOutputStream());

        return userRequestHandler.start(bpmnProcess, user);
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequest start(final String bpmnProcess) {
        return doStart(bpmnProcess, userDAO.findByUsername(AuthContextUtils.getUsername()));
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.USER_REQUEST_START + "')")
    public UserRequest start(final String bpmnProcess, final String userKey) {
        return doStart(bpmnProcess, userDAO.authFind(userKey));
    }

    protected void securityChecks(final String username, final String entitlement, final String errorMessage) {
        if (!AuthContextUtils.getUsername().equals(username)
                && !AuthContextUtils.getAuthorities().stream().
                        anyMatch(auth -> entitlement.equals(auth.getAuthority()))) {

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add(errorMessage);
            throw sce;
        }
    }

    @PreAuthorize("isAuthenticated()")
    public void cancel(final String executionId, final String reason) {
        Pair<ProcessInstance, String> parsed = userRequestHandler.parse(executionId);

        securityChecks(userDAO.find(parsed.getRight()).getUsername(),
                FlowableEntitlement.USER_REQUEST_CANCEL,
                "Canceling " + executionId + " not allowed");

        userRequestHandler.cancel(parsed.getLeft(), reason);
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequestForm claimForm(final String taskId) {
        UserRequestForm form = userRequestHandler.claimForm(taskId);
        securityChecks(form.getUsername(),
                FlowableEntitlement.USER_REQUEST_FORM_CLAIM,
                "Claiming form " + taskId + " not allowed");
        return form;
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequestForm unclaimForm(final String taskId) {
        UserRequestForm form = userRequestHandler.unclaimForm(taskId);
        securityChecks(form.getUsername(),
                FlowableEntitlement.USER_REQUEST_FORM_UNCLAIM,
                "Unclaiming form " + taskId + " not allowed");
        return form;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Pair<Integer, List<UserRequestForm>> getForms(
            final String userKey,
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        if (userKey == null) {
            securityChecks(null,
                    FlowableEntitlement.USER_REQUEST_FORM_LIST,
                    "Listing forms not allowed");
        } else {
            User user = userDAO.find(userKey);
            if (user == null) {
                throw new NotFoundException("User " + userKey);
            }

            securityChecks(user.getUsername(),
                    FlowableEntitlement.USER_REQUEST_FORM_LIST,
                    "Listing forms for user" + user.getUsername() + " not allowed");
        }

        return userRequestHandler.getForms(userKey, page, size, orderByClauses);
    }

    @PreAuthorize("isAuthenticated()")
    public UserTO submitForm(final UserRequestForm form) {
        if (form.getUsername() == null) {
            securityChecks(null,
                    FlowableEntitlement.USER_REQUEST_FORM_SUBMIT,
                    "Submitting forms not allowed");
        } else {
            securityChecks(form.getUsername(),
                    FlowableEntitlement.USER_REQUEST_FORM_SUBMIT,
                    "Submitting forms for user" + form.getUsername() + " not allowed");
        }

        WorkflowResult<UserPatch> wfResult = userRequestHandler.submitForm(form);

        // propByRes can be made empty by the workflow definition if no propagation should occur 
        // (for example, with rejected users)
        if (wfResult.getPropByRes() != null && !wfResult.getPropByRes().isEmpty()) {
            List<PropagationTaskInfo> taskInfos = propagationManager.getUserUpdateTasks(
                    new WorkflowResult<>(
                            Pair.of(wfResult.getResult(), Boolean.TRUE),
                            wfResult.getPropByRes(),
                            wfResult.getPerformedTasks()));

            taskExecutor.execute(taskInfos, false);
        }

        UserTO userTO;
        if (userDAO.find(wfResult.getResult().getKey()) == null) {
            userTO = new UserTO();
            userTO.setKey(wfResult.getResult().getKey());
        } else {
            userTO = binder.getUserTO(wfResult.getResult().getKey());
        }
        return userTO;
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
