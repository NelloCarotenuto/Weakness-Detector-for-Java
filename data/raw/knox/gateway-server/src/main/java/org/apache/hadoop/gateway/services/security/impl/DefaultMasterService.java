/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway.services.security.impl;

import org.apache.hadoop.gateway.config.GatewayConfig;
import org.apache.hadoop.gateway.i18n.messages.MessagesFactory;
import org.apache.hadoop.gateway.services.ServiceLifecycleException;
import org.apache.hadoop.gateway.services.security.MasterService;
import org.apache.hadoop.gateway.services.Service;
import org.apache.hadoop.gateway.GatewayMessages;

import java.io.File;
import java.util.Map;

public class DefaultMasterService extends CMFMasterService implements MasterService, Service {
  private static final GatewayMessages LOG = MessagesFactory.get( GatewayMessages.class );

  public DefaultMasterService() {
    super("gateway");
  }

  @Override
  public void init(GatewayConfig config, Map<String,String> options) throws ServiceLifecycleException {
    // for testing only
    if (options.containsKey("master")) {
      this.master = options.get("master").toCharArray();
    }
    else {
      boolean persisting = options.get( "persist-master").equals("true");
      String securityDir = config.getGatewayHomeDir() + File.separator + "conf" + File.separator + "security";
      String filename = "master";
      setupMasterSecret(securityDir, filename, persisting);
    }
  }
  

  @Override
  public void start() throws ServiceLifecycleException {
  }

  @Override
  public void stop() throws ServiceLifecycleException {
  }
  
}
