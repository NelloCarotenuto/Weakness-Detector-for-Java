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
package org.apache.hadoop.gateway;

import org.apache.commons.cli.ParseException;
import org.apache.hadoop.gateway.i18n.messages.Message;
import org.apache.hadoop.gateway.i18n.messages.MessageLevel;
import org.apache.hadoop.gateway.i18n.messages.Messages;
import org.apache.hadoop.gateway.i18n.messages.StackTrace;
import org.apache.hadoop.gateway.services.security.KeystoreServiceException;
import org.apache.hadoop.gateway.util.urltemplate.Template;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.Map;

/**
 *
 */
@Messages(logger="org.apache.hadoop.gateway")
public interface GatewayMessages {

  @Message( level = MessageLevel.FATAL, text = "Failed to parse command line: {0}" )
  void failedToParseCommandLine( @StackTrace( level = MessageLevel.DEBUG ) ParseException e );

  @Message( level = MessageLevel.INFO, text = "Starting gateway..." )
  void startingGateway();

  @Message( level = MessageLevel.FATAL, text = "Failed to start gateway: {0}" )
  void failedToStartGateway( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.INFO, text = "Started gateway on port {0}." )
  void startedGateway( int port );

  @Message( level = MessageLevel.INFO, text = "Stopping gateway..." )
  void stoppingGateway();

  @Message( level = MessageLevel.INFO, text = "Stopped gateway." )
  void stoppedGateway();

  @Message( level = MessageLevel.INFO, text = "Loading configuration resource {0}" )
  void loadingConfigurationResource( String res );

  @Message( level = MessageLevel.INFO, text = "Loading configuration file {0}" )
  void loadingConfigurationFile( String file );

  @Message( level = MessageLevel.WARN, text = "Failed to load configuration file {0}: {1}" )
  void failedToLoadConfig( String path, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.INFO, text = "Using {1} as GATEWAY_HOME via {0}." )
  void settingGatewayHomeDir( String location, String home );

  @Message( level = MessageLevel.INFO, text = "Loading topologies from directory: {0}" )
  void loadingTopologiesFromDirecotry( String topologiesDir );

  @Message( level = MessageLevel.DEBUG, text = "Loading topology file: {0}" )
  void loadingTopologyFile( String fileName );

  @Message( level = MessageLevel.INFO, text = "Monitoring topologies in directory: {0}" )
  void monitoringTopologyChangesInDirectory( String topologiesDir );

  @Message( level = MessageLevel.INFO, text = "Deploying topology {0} to {1}" )
  void deployingTopology( String clusterName, String warDirName );

  @Message( level = MessageLevel.INFO, text = "Deployed topology {0}." )
  void deployedTopology( String clusterName );

  @Message( level = MessageLevel.INFO, text = "Loading topology {0} from {1}" )
  void redeployingTopology( String clusterName, String warDirName );

  @Message( level = MessageLevel.INFO, text = "Redeployed topology {0}." )
  void redeployedTopology( String clusterName );

  @Message( level = MessageLevel.ERROR, text = "Failed to deploy topology {0}: {1}" )
  void failedToDeployTopology( String name, @StackTrace(level=MessageLevel.DEBUG) Throwable e );

  @Message( level = MessageLevel.ERROR, text = "Failed to undeploy topology {0}: {1}" )
  void failedToUndeployTopology( String name, @StackTrace(level=MessageLevel.DEBUG) Exception e );

  @Message( level = MessageLevel.INFO, text = "Deleting deployed topology {0}" )
  void deletingDeployment( String warDirName );

  @Message( level = MessageLevel.INFO, text = "Creating gateway home directory: {0}" )
  void creatingGatewayHomeDir( File homeDir );

  @Message( level = MessageLevel.INFO, text = "Creating gateway deployment directory: {0}" )
  void creatingGatewayDeploymentDir( File topologiesDir );

  @Message( level = MessageLevel.INFO, text = "Creating default gateway configuration file: {0}" )
  void creatingDefaultConfigFile( File defaultConfigFile );

  @Message( level = MessageLevel.INFO, text = "Creating sample topology file: {0}" )
  void creatingDefaultTopologyFile( File defaultConfigFile );

  @Message( level = MessageLevel.WARN, text = "Ignoring service deployment contributor with invalid null name: {0}" )
  void ignoringServiceContributorWithMissingName( String className );

  @Message( level = MessageLevel.WARN, text = "Ignoring service deployment contributor with invalid null role: {0}" )
  void ignoringServiceContributorWithMissingRole( String className );

  @Message( level = MessageLevel.WARN, text = "Ignoring provider deployment contributor with invalid null name: {0}" )
  void ignoringProviderContributorWithMissingName( String className );

  @Message( level = MessageLevel.WARN, text = "Ignoring provider deployment contributor with invalid null role: {0}" )
  void ignoringProviderContributorWithMissingRole( String className );

  @Message( level = MessageLevel.INFO, text = "Loaded logging configuration: {0}" )
  void loadedLoggingConfig( String fileName );

  @Message( level = MessageLevel.WARN, text = "Failed to load logging configuration: {0}" )
  void failedToLoadLoggingConfig( String fileName );

  @Message( level = MessageLevel.INFO, text = "Creating credential store for the gateway instance." )
  void creatingCredentialStoreForGateway();

  @Message( level = MessageLevel.INFO, text = "Credential store for the gateway instance found - no need to create one." )
  void credentialStoreForGatewayFoundNotCreating();

  @Message( level = MessageLevel.INFO, text = "Creating keystore for the gateway instance." )
  void creatingKeyStoreForGateway();

  @Message( level = MessageLevel.INFO, text = "Keystore for the gateway instance found - no need to create one." )
  void keyStoreForGatewayFoundNotCreating();

  @Message( level = MessageLevel.INFO, text = "Creating credential store for the cluster: {0}" )
  void creatingCredentialStoreForCluster(String clusterName);

  @Message( level = MessageLevel.INFO, text = "Credential store found for the cluster: {0} - no need to create one." )
  void credentialStoreForClusterFoundNotCreating(String clusterName);

  @Message( level = MessageLevel.DEBUG, text = "Received request: {0} {1}" )
  void receivedRequest( String method, Template uri );

  @Message( level = MessageLevel.DEBUG, text = "Dispatch request: {0} {1}" )
  void dispatchRequest( String method, URI uri );
  
  @Message( level = MessageLevel.WARN, text = "Connection exception dispatching request: {0} {1}" )
  void dispatchServiceConnectionException( URI uri, @StackTrace(level=MessageLevel.WARN) Exception e );

  @Message( level = MessageLevel.DEBUG, text = "Signature verified: {0}" )
  void signatureVerified( boolean verified );

  @Message( level = MessageLevel.DEBUG, text = "Apache Knox Gateway {0} ({1})" )
  void gatewayVersionMessage( String version, String hash );

  @Message( level = MessageLevel.ERROR, text = "Failed to inject service {0}: {1}" )
  void failedToInjectService( String serviceName, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to finalize contribution: {0}" )
  void failedToFinalizeContribution( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to contribute service [role={1}, name={0}]: {2}" )
  void failedToContributeService( String name, String role, @StackTrace( level = MessageLevel.ERROR ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to contribute provider [role={1}, name={0}]: {2}" )
  void failedToContributeProvider( String name, String role, @StackTrace( level = MessageLevel.ERROR ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to initialize contribution: {0}" )
  void failedToInitializeContribution( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to initialize servlet instace: {0}" )
  void failedToInitializeServletInstace( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to execute filter: {0}" )
  void failedToExecuteFilter( @StackTrace( level = MessageLevel.DEBUG ) Throwable t );

  @Message( level = MessageLevel.ERROR, text = "Failed to load topology {0}: {1}")
  void failedToLoadTopology( String fileName, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to handle topology events: {0}" )
  void failedToHandleTopologyEvents( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to reload topologies: {0}" )
  void failedToReloadTopologies( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.FATAL, text = "Unsupported encoding: {0}" )
  void unsupportedEncoding( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to persist master secret: {0}" )
  void failedToPersistMasterSecret( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to encrypt master secret: {0}" )
  void failedToEncryptMasterSecret( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to initialize master service from persistent master {0}: {1}" )
  void failedToInitializeFromPersistentMaster( String masterFileName, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to encode passphrase: {0}" )
  void failedToEncodePassphrase( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to verify signature: {0}")
  void failedToVerifySignature( @StackTrace(level=MessageLevel.DEBUG) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to sign the data: {0}")
  void failedToSignData( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to decrypt password for cluster {0}: {1}" )
  void failedToDecryptPasswordForCluster( String clusterName, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to encrypt password for cluster {0}: {1}")
  void failedToEncryptPasswordForCluster( String clusterName, @StackTrace( level = MessageLevel.DEBUG ) Exception e );
  
  @Message( level = MessageLevel.ERROR, text = "Failed to create keystore [filename={0}, type={1}]: {2}" )
  void failedToCreateKeystore( String fileName, String keyStoreType, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to load keystore [filename={0}, type={1}]: {2}" )
  void failedToLoadKeystore( String fileName, String keyStoreType, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to add key for cluster {0}: {1}" )
  void failedToAddKeyForCluster( String clusterName, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to add credential for cluster {0}: {1}" )
  void failedToAddCredentialForCluster( String clusterName, @StackTrace( level = MessageLevel.DEBUG ) Exception e );
  
  @Message( level = MessageLevel.ERROR, text = "Failed to get key for Gateway {0}: {1}" )
  void failedToGetKeyForGateway( String alias, @StackTrace( level=MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to get credential for cluster {0}: {1}" )
  void failedToGetCredentialForCluster( String clusterName, @StackTrace(level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to get key for cluster {0}: {1}" )
  void failedToGetKeyForCluster( String clusterName, @StackTrace(level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to add self signed certificate for Gateway {0}: {1}" )
  void failedToAddSeflSignedCertForGateway( String alias, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to generate secret key from password: {0}" )
  void failedToGenerateKeyFromPassword( @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to establish connection to {0}: {1}" )
  void failedToEstablishConnectionToUrl( String url, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to interpret property \"{0}\": {1}")
  void failedToInterpretProperty( String property, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to instantiate the internal gateway services." )
  void failedToInstantiateGatewayServices();

  @Message( level = MessageLevel.ERROR, text = "Failed to serialize map to Json string {0}: {1}" )
  void failedToSerializeMapToJSON( Map<String, Object> map, @StackTrace( level = MessageLevel.DEBUG ) Exception e );

  @Message( level = MessageLevel.ERROR, text = "Failed to get map from Json string {0}: {1}" )
  void failedToGetMapFromJsonString( String json, @StackTrace( level = MessageLevel.DEBUG ) Exception e );
  
  @Message( level = MessageLevel.DEBUG, text = "Successful Knox->Hadoop SPNegotiation authentication for URL: {0}" )
  void successfulSPNegoAuthn(String uri);
  
  @Message( level = MessageLevel.ERROR, text = "Failed Knox->Hadoop SPNegotiation authentication for URL: {0}" )
  void failedSPNegoAuthn(String uri);

  @Message( level = MessageLevel.DEBUG, text = "Dispatch response status: {0}" )
  void dispatchResponseStatusCode(int statusCode);

  @Message( level = MessageLevel.DEBUG, text = "Dispatch response status: {0}, Location: {1}" )
  void dispatchResponseCreatedStatusCode( int statusCode, String location );

  @Message( level = MessageLevel.ERROR, text = "Failed to decrypt cipher text for cluster {0}: due to inability to retrieve the password." )
  void failedToDecryptCipherForClusterNullPassword(String clusterName);

  @Message( level = MessageLevel.DEBUG, text = "Gateway services have not been initialized." )
  void gatewayServicesNotInitialized();

  @Message( level = MessageLevel.INFO, text = "The Gateway SSL certificate is issued to hostname: {0}." )
  void certificateHostNameForGateway(String cn);

  @Message( level = MessageLevel.INFO, text = "The Gateway SSL certificate is valid between: {0} and {1}." )
  void certificateValidityPeriod(Date notBefore, Date notAfter);

  @Message( level = MessageLevel.ERROR, text = "Unable to retrieve certificate for Gateway: {0}." )
  void unableToRetrieveCertificateForGateway(Exception e);

  @Message( level = MessageLevel.ERROR, text = "Failed to generate alias for cluster: {0} {1}." )
  void failedToGenerateAliasForCluster(String clusterName, KeystoreServiceException e);

  @Message( level = MessageLevel.DEBUG, text = "Key passphrase not found in credential store - using master secret." )
  void assumingKeyPassphraseIsMaster();

}
