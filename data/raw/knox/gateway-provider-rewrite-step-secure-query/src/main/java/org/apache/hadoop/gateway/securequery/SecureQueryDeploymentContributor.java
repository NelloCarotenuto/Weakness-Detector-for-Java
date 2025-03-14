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
package org.apache.hadoop.gateway.securequery;

import org.apache.hadoop.gateway.deploy.DeploymentContext;
import org.apache.hadoop.gateway.deploy.ProviderDeploymentContributor;
import org.apache.hadoop.gateway.deploy.ProviderDeploymentContributorBase;
import org.apache.hadoop.gateway.descriptor.FilterParamDescriptor;
import org.apache.hadoop.gateway.descriptor.ResourceDescriptor;
import org.apache.hadoop.gateway.services.security.AliasService;
import org.apache.hadoop.gateway.topology.Provider;
import org.apache.hadoop.gateway.topology.Service;

import java.util.List;

public class SecureQueryDeploymentContributor
    extends ProviderDeploymentContributorBase
    implements ProviderDeploymentContributor {

  public static final String PROVIDER_ROLE_NAME = "secure-query";
  public static final String PROVIDER_IMPL_NAME = "default";
  private AliasService as;

  @Override
  public String getRole() {
    return PROVIDER_ROLE_NAME;
  }

  @Override
  public String getName() {
    return PROVIDER_IMPL_NAME;
  }
  
  public void setAliasService(AliasService as) {
    this.as = as;
  }

  @Override
  public void initializeContribution(DeploymentContext context) {
    super.initializeContribution(context);

    String clusterName = context.getTopology().getName();
    this.as.generateAliasForCluster(clusterName, "encryptQueryString");
  }

  @Override
  public void contributeProvider( DeploymentContext context, Provider provider ) {
    if( provider.isEnabled() ) {
//      UrlRewriteRulesDescriptor rules = context.getDescriptor( REWRITE_ROLE_NAME );
//      if( rules != null ) {
//        HostmapFunctionDescriptor func = rules.addFunction( HostmapFunctionDescriptor.FUNCTION_NAME );
//        if( func != null ) {
//          Asset asset = createAsset( provider );
//          context.getWebArchive().addAsWebInfResource(
//              asset, HostmapFunctionProcessor.DESCRIPTOR_DEFAULT_FILE_NAME );
//          func.config( HostmapFunctionProcessor.DESCRIPTOR_DEFAULT_LOCATION );
//        }
//      }
    }
  }

//  private Asset createAsset( Provider provider ) {
//    StringWriter buffer = new StringWriter();
//    PrintWriter writer = new PrintWriter( buffer );
//    for( Map.Entry<String,String> entry : provider.getParams().entrySet() ) {
//      String externalHosts = entry.getKey();
//      String internalHosts = entry.getValue();
//      writer.print( externalHosts );
//      writer.print( "=" );
//      writer.println( internalHosts ) ;
//    }
//    writer.close();
//    String string = buffer.toString();
//    Asset asset = new StringAsset( string );
//    return asset;
//  }

  @Override
  public void contributeFilter(
      DeploymentContext context,
      Provider provider,
      Service service,
      ResourceDescriptor resource,
      List<FilterParamDescriptor> params ) {
  }

}
