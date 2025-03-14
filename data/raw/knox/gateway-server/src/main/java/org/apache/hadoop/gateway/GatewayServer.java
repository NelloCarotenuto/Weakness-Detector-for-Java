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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.gateway.config.GatewayConfig;
import org.apache.hadoop.gateway.config.impl.GatewayConfigImpl;
import org.apache.hadoop.gateway.deploy.DeploymentFactory;
import org.apache.hadoop.gateway.i18n.messages.MessagesFactory;
import org.apache.hadoop.gateway.i18n.resources.ResourcesFactory;
import org.apache.hadoop.gateway.services.GatewayServices;
import org.apache.hadoop.gateway.services.ServiceLifecycleException;
import org.apache.hadoop.gateway.services.registry.ServiceRegistry;
import org.apache.hadoop.gateway.services.security.SSLService;
import org.apache.hadoop.gateway.topology.Topology;
import org.apache.hadoop.gateway.topology.TopologyEvent;
import org.apache.hadoop.gateway.topology.TopologyListener;
import org.apache.hadoop.gateway.topology.file.FileTopologyProvider;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class GatewayServer {
  private static GatewayResources res = ResourcesFactory.get( GatewayResources.class );
  private static GatewayMessages log = MessagesFactory.get( GatewayMessages.class );
  private static GatewayServer server;
  private static GatewayServices services;
  
  private static Properties buildProperties;

  private Server jetty;
  private GatewayConfig config;
  private ContextHandlerCollection contexts;
  private FileTopologyProvider monitor;
  private TopologyListener listener;
  private Map<String, WebAppContext> deployments;

  public static void main( String[] args ) {
    try {
      CommandLine cmd = GatewayCommandLine.parse( args );
      if( cmd.hasOption( GatewayCommandLine.HELP_LONG ) ) {
        GatewayCommandLine.printHelp();
      } else if( cmd.hasOption( GatewayCommandLine.VERSION_LONG ) ) {
        buildProperties = loadBuildProperties();
        System.out.println( res.gatewayVersionMessage( // I18N not required.
            buildProperties.getProperty( "build.version", "unknown" ),
            buildProperties.getProperty( "build.hash", "unknown" ) ) );
      } else {
        services = instantiateGatewayServices();
        if (services == null) {
          log.failedToInstantiateGatewayServices();
        }
        GatewayConfig config = new GatewayConfigImpl();
        configureLogging( config );
        if (config.isHadoopKerberosSecured()) {
          configureKerberosSecurity( config );
        }
        Map<String,String> options = new HashMap<String,String>();
        options.put(GatewayCommandLine.PERSIST_LONG, Boolean.toString(cmd.hasOption(GatewayCommandLine.PERSIST_LONG)));
        services.init(config, options);
        if (!cmd.hasOption(GatewayCommandLine.NOSTART_LONG)) {
          startGateway( config, services );
        }
      }
    } catch( ParseException e ) {
      log.failedToParseCommandLine( e );
    } catch (ServiceLifecycleException e) {
      log.failedToStartGateway( e );
    }
  }

  private static GatewayServices instantiateGatewayServices() {
    ServiceLoader<GatewayServices> loader = ServiceLoader.load( GatewayServices.class );
    Iterator<GatewayServices> services = loader.iterator();
    if (services.hasNext()) {
      return services.next();
    }
    return null;
  }

  public static synchronized GatewayServices getGatewayServices() {
    return services;
  }

  private static void configureLogging( GatewayConfig config ) {
    String fileName = config.getGatewayHomeDir() + "/conf/log4j.properties";
    File file = new File( fileName );
    if( file.isFile() && file.canRead() ) {
      FileInputStream stream;
      try {
        stream = new FileInputStream( file );
        Properties properties = new Properties();
        properties.load( stream );
        PropertyConfigurator.configure( properties );
        log.loadedLoggingConfig( fileName );
      } catch( IOException e ) {
        log.failedToLoadLoggingConfig( fileName );
      }
    }
  }

  private static void configureKerberosSecurity( GatewayConfig config ) {
    System.setProperty(GatewayConfig.HADOOP_KERBEROS_SECURED, "true");
    System.setProperty(GatewayConfig.KRB5_CONFIG, config.getKerberosConfig());
    System.setProperty(GatewayConfig.KRB5_DEBUG, 
        Boolean.toString(config.isKerberosDebugEnabled()));
    System.setProperty(GatewayConfig.KRB5_LOGIN_CONFIG, config.getKerberosLoginConfig());
    System.setProperty(GatewayConfig.KRB5_USE_SUBJECT_CREDS_ONLY,  "false");
  }
  
  private static Properties loadBuildProperties() {
    Properties properties = new Properties();
    InputStream inputStream = GatewayServer.class.getClassLoader().getResourceAsStream( "build.properties" );
    if( inputStream != null ) {
      try {
        properties.load( inputStream );
        inputStream.close();
      } catch( IOException e ) {
        // Ignore.
      }
    }
    return properties;
  }

  private static void extractToFile( String resource, File file ) throws IOException {
    InputStream input = ClassLoader.getSystemResourceAsStream( resource );
    OutputStream output = new FileOutputStream( file );
    IOUtils.copy( input, output );
    output.close();
    input.close();
  }

  public static GatewayServer startGateway( GatewayConfig config, GatewayServices svcs ) {
    try {
      log.startingGateway();
      server = new GatewayServer( config );
      synchronized ( server ) {
        if (services == null) {
          services = svcs;
        }
        services.start();
        DeploymentFactory.setGatewayServices(services);
        server.start();
        log.startedGateway( server.jetty.getConnectors()[ 0 ].getLocalPort() );
        return server;
      }
    } catch( Exception e ) {
      log.failedToStartGateway( e );
      return null;
    }
  }

  public GatewayServer( GatewayConfig config ) {
    this(config, null);
  }

  public GatewayServer( GatewayConfig config, Properties options ) {
      this.config = config;
      this.listener = new InternalTopologyListener();
  }

//  private void setupSslExample() throws Exception {
//    SslContextFactory sslContextFactory = new SslContextFactory( true );
//    sslContextFactory.setCertAlias( "server" );
//    sslContextFactory.setKeyStorePath( "target/test-classes/server-keystore.jks" );
//    sslContextFactory.setKeyStorePassword( "password" );
//    //sslContextFactory.setKeyManagerPassword( "password" );
//    sslContextFactory.setTrustStore( "target/test-classes/server-truststore.jks" );
//    sslContextFactory.setTrustStorePassword( "password" );
//    sslContextFactory.setNeedClientAuth( false );
//    sslContextFactory.setTrustAll( true );
//    SslConnector sslConnector = new SslSelectChannelConnector( sslContextFactory );
//
//    ServletContextHandler context = new ServletContextHandler( ServletContextHandler.SESSIONS );
//    context.setContextPath( "/" );
//    ServletHolder servletHolder = new ServletHolder( new MockServlet() );
//    context.addServlet( servletHolder, "/*" );
//
//    jetty = new Server();
//    jetty.addConnector( sslConnector );
//    jetty.setHandler( context );
//    jetty.start();
//  }

  
  private synchronized void start() throws Exception {

    // Create the global context handler.
    contexts = new ContextHandlerCollection();

    // A map to keep track of current deployments by cluster name.
    deployments = new ConcurrentHashMap<String, WebAppContext>();

    // Determine the socket address and check availability.
    InetSocketAddress address = config.getGatewayAddress();
    checkAddressAvailability( address );

    // Start Jetty.
    if (config.isSSLEnabled()) {
      jetty = new Server();
    }
    else {
      jetty = new Server(address);
    }
    if (config.isSSLEnabled()) {
      SSLService ssl = services.getService("SSLService");
      Connector connector = (Connector) ssl.buildSSlConnector(config.getGatewayHomeDir());
      connector.setHost(address.getHostName());
      connector.setPort(address.getPort());
      jetty.addConnector(connector);
    }
    jetty.setHandler( contexts );
    try {
    jetty.start();
    }
    catch (IOException e) {
      log.failedToStartGateway( e );
      throw e;
    }

    // Create a dir/file based cluster topology provider.
    File topologiesDir = calculateAbsoluteTopologiesDir();
    monitor = new FileTopologyProvider( topologiesDir );
    monitor.addTopologyChangeListener( listener );

    // Load the current topologies.
    log.loadingTopologiesFromDirecotry( topologiesDir.getAbsolutePath() );
    monitor.reloadTopologies();

    // Start the topology monitor.
    log.monitoringTopologyChangesInDirectory( topologiesDir.getAbsolutePath() );
    monitor.startMonitor();
  }

  public synchronized void stop() throws Exception {
    log.stoppingGateway();
    services.stop();
    monitor.stopMonitor();
    jetty.stop();
    jetty.join();
    log.stoppedGateway();
  }

  public InetSocketAddress[] getAddresses() {
    InetSocketAddress[] addresses = new InetSocketAddress[ jetty.getConnectors().length ];
    for( int i=0, n=addresses.length; i<n; i++ ) {
      Connector connector = jetty.getConnectors()[ i ];
      String host = connector.getHost();
      if( host == null ) {
        addresses[ i ] = new InetSocketAddress( connector.getLocalPort() );
      } else {
        addresses[ i ] = new InetSocketAddress( host, connector.getLocalPort() );
      }
    }
    return addresses;
  }

  private synchronized void internalDeploy( Topology topology, File warFile ) {
    String name = topology.getName();
    String warPath = warFile.getAbsolutePath();
    WebAppContext context = new WebAppContext();
    context.setDefaultsDescriptor( null );
    context.setContextPath( "/" + config.getGatewayPath() + "/" + name );
    context.setWar( warPath );
    // internalUndeploy( topology ); KNOX-152
    context.setAttribute( GatewayServices.GATEWAY_CLUSTER_ATTRIBUTE, name );
    deployments.put( name, context );
    contexts.addHandler( context );
    try {
      context.start();
    } catch( Exception e ) {
      log.failedToDeployTopology( name, e );
    }
  }

  private synchronized void internalUndeploy( Topology topology ) {
    WebAppContext context = deployments.remove( topology.getName() );
    if( context != null ) {
      ServiceRegistry sr = getGatewayServices().getService(GatewayServices.SERVICE_REGISTRY_SERVICE);
      if (sr != null) {
        sr.removeClusterServices(topology.getName());
      }
      contexts.removeHandler( context ) ;
      try {
        context.stop();
      } catch( Exception e ) {
        log.failedToUndeployTopology( topology.getName(), e );
      }
    }
  }

  // Using an inner class to hide the handleTopologyEvent method from consumers of GatewayServer.
  private class InternalTopologyListener implements TopologyListener {

    @Override
    public void handleTopologyEvent( List<TopologyEvent> events ) {
      synchronized ( GatewayServer.this ) {
        for( TopologyEvent event : events ) {
          Topology topology = event.getTopology();
          File topoDir = calculateAbsoluteTopologiesDir();
          File warDir = calculateDeploymentDir( topology );
          if( event.getType().equals( TopologyEvent.Type.DELETED ) ) {
            File[] files = topoDir.listFiles( new WarDirFilter( topology.getName() + "\\.war\\.[0-9A-Fa-f]+" ) );
            if( files != null ) {
              for( File file : files ) {
                log.deletingDeployment( file.getAbsolutePath() );
                internalUndeploy( topology );
                FileUtils.deleteQuietly( file );
              }
            }
          } else {
            try {
              if( !warDir.exists() ) {
                log.deployingTopology( topology.getName(), warDir.getAbsolutePath() );
                internalUndeploy( topology ); // KNOX-152
                WebArchive war = null;
                war = DeploymentFactory.createDeployment( config, topology );
                File tmp = war.as( ExplodedExporter.class ).exportExploded( topoDir, warDir.getName() + ".tmp" );
                tmp.renameTo( warDir );
                internalDeploy( topology, warDir );
                //log.deployedTopology( topology.getName());
              } else {
                log.redeployingTopology( topology.getName(), warDir.getAbsolutePath() );
                internalDeploy( topology, warDir );
                //log.redeployedTopology( topology.getName() );
              }
            } catch( Throwable e ) {
              log.failedToDeployTopology( topology.getName(), e );
            }
          }
        }
      }
    }

  }

  private static File calculateAbsoluteTopologiesDir( GatewayConfig config ) {
    File topoDir = new File( config.getGatewayHomeDir(), config.getDeploymentDir() );
    topoDir = topoDir.getAbsoluteFile();
    return topoDir;
  }

  private File calculateAbsoluteTopologiesDir() {
    return calculateAbsoluteTopologiesDir( config );
  }

  private File calculateDeploymentDir( Topology topology ) {
    File warDir = new File( calculateAbsoluteTopologiesDir(), calculateDeploymentName( topology ) );
    return warDir;
  }

  private String calculateDeploymentName( Topology topology ) {
    String name = topology.getName() + ".war." + Long.toHexString( topology.getTimestamp() );
    return name;
  }

  private static void checkAddressAvailability( InetSocketAddress address ) throws IOException {
    ServerSocket socket = new ServerSocket();
    socket.bind( address );
    socket.close();
  }

  private class WarDirFilter implements FilenameFilter {

    Pattern pattern;

    WarDirFilter( String regex ) {
      pattern = Pattern.compile( regex );
    }

    @Override
    public boolean accept( File dir, String name ) {
      return pattern.matcher( name ).matches();
    }
  }

}
