/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.hadoop.gateway.shell.hbase.table.row;

public class Column {

  private String family;
  private String qualifier;

  public Column( String family, String qualifier ) {
    this.family = family;
    this.qualifier = qualifier;
  }

  public Column( String family ) {
    this( family, null );
  }

  public String family() {
    return family;
  }

  public String qualifier() {
    return qualifier;
  }

  public String toURIPart() {
    StringBuilder sb = new StringBuilder( family );
    if( qualifier != null && !qualifier.isEmpty() ) {
      sb.append( ":" ).append( qualifier );
    }
    return sb.toString();
  }
}
