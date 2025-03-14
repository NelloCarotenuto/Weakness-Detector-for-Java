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
package org.apache.hadoop.gateway.launcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;

class Invoker {

  private static String[] STRING_ARRAY = new String[0];
  private static Class STRING_ARRAY_CLASS = STRING_ARRAY.getClass();

  static void invoke( Command command ) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    setSysProps( command.props );
    ClassLoader mainLoader = createClassLoader( command.classPath );
    Class mainClass = loadMainClass( mainLoader, command.mainClass );
    Method mainMethod = findMainMethod( mainClass, command.mainMethod );
    invokeMainMethod( mainLoader, mainMethod, command.mainArgs );
  }

  private static void setSysProps( Properties properties ) {
    for( String name : properties.stringPropertyNames() ) {
      System.setProperty( name, properties.getProperty( name ) );
    }
  }

  private static ClassLoader createClassLoader( List<URL> urls ) {
    URL[] urlArray = new URL[ urls.size() ];
    urls.toArray( urlArray );
    return new URLClassLoader( urlArray );
  }

  private static Class loadMainClass( ClassLoader loader, String className ) throws ClassNotFoundException {
    Class mainClass = loader.loadClass( className );
    return mainClass;
  }

  private static Method findMainMethod( Class mainClass, String methodName ) throws NoSuchMethodException {
    Method method = mainClass.getMethod( methodName, STRING_ARRAY_CLASS );
    return method;
  }

  private static void invokeMainMethod( ClassLoader loader, Method method, String[] args ) throws InvocationTargetException, IllegalAccessException {
    Thread.currentThread().setContextClassLoader( loader );
    method.invoke( method.getClass(), (Object)args );
  }

}
