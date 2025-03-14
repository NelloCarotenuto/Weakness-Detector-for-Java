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
package org.apache.hadoop.gateway.filter.rewrite.impl.xml;

import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteFilterApplyDescriptor;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteFilterBufferDescriptor;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteFilterContentDescriptor;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteFilterDetectDescriptor;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteFilterGroupDescriptor;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteFilterPathDescriptor;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteFilterScopeDescriptor;
import org.apache.hadoop.gateway.filter.rewrite.i18n.UrlRewriteMessages;
import org.apache.hadoop.gateway.filter.rewrite.i18n.UrlRewriteResources;
import org.apache.hadoop.gateway.i18n.messages.MessagesFactory;
import org.apache.hadoop.gateway.i18n.resources.ResourcesFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Pattern;

public abstract class XmlFilterReader extends Reader {

  private static UrlRewriteResources RES = ResourcesFactory.get( UrlRewriteResources.class );

  private static String DEFAULT_XML_VERSION = "1.0";

  private static final UrlRewriteMessages LOG = MessagesFactory.get( UrlRewriteMessages.class );
  private static final UrlRewriteFilterPathDescriptor.Compiler<XPathExpression> XPATH_COMPILER = new XmlPathCompiler();
  private static final UrlRewriteFilterPathDescriptor.Compiler<Pattern> REGEX_COMPILER = new RegexCompiler();

  private Reader reader;
  private UrlRewriteFilterContentDescriptor config;
  private int offset;
  private StringWriter writer;
  private StringBuffer buffer;
  private XMLInputFactory factory;
  private XMLEventReader parser;
  private Document document;
  private Stack<Level> stack;

  protected XmlFilterReader( Reader reader, UrlRewriteFilterContentDescriptor config ) throws IOException, XMLStreamException {
    this.reader = reader;
    this.config = config;
    writer = new StringWriter();
    buffer = writer.getBuffer();
    offset = 0;
    document = null;
    stack = new Stack<Level>();
    factory = XMLInputFactory.newFactory();
    parser = factory.createXMLEventReader( reader );
  }

  protected abstract String filterAttribute( QName elementName, QName attributeName, String attributeValue, String ruleName );

  protected abstract String filterText( QName elementName, String text, String ruleName );

  @Override
  public int read( char[] destBuffer, int destOffset, int destCount ) throws IOException {
    int count = 0;
    int available = buffer.length() - offset;

    if( available == 0 ) {
      if( parser.hasNext() ) {
        try {
          XMLEvent event = parser.nextEvent();
          processEvent( event );
        } catch( RuntimeException e ) {
          throw e;
        } catch( Exception e ) {
          throw e instanceof IOException ? (IOException)e : new IOException( e );
        }
        available = buffer.length() - offset;
      } else {
        count = -1;
      }
    }

    if( available > 0 ) {
      count = Math.min( destCount, available );
      buffer.getChars( offset, offset + count, destBuffer, destOffset );
      offset += count;
      if( offset == buffer.length() ) {
        offset = 0;
        buffer.setLength( 0 );
      }
    }
    return count;
  }

  private void processEvent( XMLEvent event ) throws ParserConfigurationException, XPathExpressionException, IOException {
    int type = event.getEventType();
    switch( type ) {
      case XMLEvent.START_DOCUMENT:
        processStartDocument( (StartDocument)event );
        break;
      case XMLEvent.END_DOCUMENT:
        processEndDocument();
        break;
      case XMLEvent.START_ELEMENT:
        processStartElement( event.asStartElement() );
        break;
      case XMLEvent.END_ELEMENT:
        processEndElement( event.asEndElement() );
        break;
      case XMLEvent.CHARACTERS:
      case XMLEvent.CDATA:
      case XMLEvent.SPACE:
        processCharacters( event.asCharacters() );
        break;
      case XMLEvent.COMMENT:
        processComment( (Comment)event );
        break;
      case XMLEvent.DTD:
      case XMLEvent.NAMESPACE:
      case XMLEvent.ATTRIBUTE:
      case XMLEvent.ENTITY_REFERENCE:
      case XMLEvent.ENTITY_DECLARATION:
      case XMLEvent.NOTATION_DECLARATION:
      case XMLEvent.PROCESSING_INSTRUCTION:
      default:
        // Fail if we run into any of these for now.
        throw new IllegalStateException( Integer.toString( type ) );
    }
  }

  private void processStartDocument( StartDocument event ) throws ParserConfigurationException {
    //System.out.println( "SD=" + event );
    String s;

    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    pushLevel( null, event, document, document, config );

    writer.write( "<?xml" );

    s = event.getVersion();
    if( s == null ) {
      s = DEFAULT_XML_VERSION;
    }
    writer.write( " version=\"");
    writer.write( s );
    writer.write( "\"" );

    s = event.getCharacterEncodingScheme();
    if( s != null ) {
      writer.write( " encoding=\"");
      writer.write( s );
      writer.write( "\"" );
    }

    writer.write( " standalone=\"");
    writer.write( event.isStandalone() ? "yes" : "no" );
    writer.write( "\"" );

    writer.write( "?>" );
  }

  private void processEndDocument() {
    stack.clear();
    document = null;
  }

  private void processStartElement( StartElement event ) throws XPathExpressionException {
    //System.out.println( "SE=" + event );

    // Create a new "empty" element and add it to the document.
    Element element = bufferElement( event );
    Level parent = stack.peek();
    parent.node.appendChild( element );

    // If already buffering just continue to do so.
    // Note: Don't currently support nested buffer or scope descriptors.
    if( currentlyBuffering() ) {
      pushLevel( parent, event, element, parent.scopeNode, parent.scopeConfig );
      bufferAttributes( event, element );
    // Else not currently buffering
    } else {
      // See if there is a matching path descriptor in the current scope.
      UrlRewriteFilterPathDescriptor descriptor = pickFirstMatchingPath( parent );
      if( descriptor != null ) {
        // If this is a buffer descriptor then switch to buffering and buffer the attributes.
        if( descriptor instanceof UrlRewriteFilterBufferDescriptor ) {
          pushLevel( parent, event, element, element, (UrlRewriteFilterBufferDescriptor)descriptor );
          bufferAttributes( event, element );
        // Otherwise if this is a scope descriptor then change the scope and stream the attributes.
        } else if( descriptor instanceof UrlRewriteFilterScopeDescriptor ) {
          pushLevel( parent, event, element, element, (UrlRewriteFilterScopeDescriptor)descriptor );
          streamElement( event, element );
        // Else found an unexpected matching path.
        } else {
          // This is likely because there is an <apply> targeted at the text of an element.
          // That "convenience" config will be taken care of in the streamElement() processing.
          pushLevel( parent, event, element, parent.scopeNode, parent.scopeConfig );
          streamElement( event, element );
        }
      // If there is no matching path descriptor then continue streaming.
      } else {
        pushLevel( parent, event, element, parent.scopeNode, parent.scopeConfig );
        streamElement( event, element );
      }
    }
  }

  private void processEndElement( EndElement event ) throws XPathExpressionException, IOException {
    //System.out.println( "EE=" + event );
    boolean buffering = currentlyBuffering();
    Level child = stack.pop();
    if( buffering ) {
      if( child.node == child.scopeNode ) {
        processBufferedElement( child );
      }
    } else {
      QName n = event.getName();
      writer.write( "</" );
      String p = n.getPrefix();
      if( p != null && !p.isEmpty() ) {
        writer.write( p );
        writer.write( ":" );
      }
      writer.write( n.getLocalPart() );
      writer.write( ">" );
      child.node.getParentNode().removeChild( child.node );
    }
  }

  private Element bufferElement( StartElement event ) {
    QName qname = event.getName();
    String prefix = qname.getPrefix();
    String uri = qname.getNamespaceURI();
    Element element;
    if( uri == null || uri.isEmpty() ) {
      element = document.createElement( qname.getLocalPart() );
    } else {
      element = document.createElementNS( qname.getNamespaceURI(), qname.getLocalPart() );
      if( prefix != null && !prefix.isEmpty() ) {
        element.setPrefix( prefix );
      }
    }
    // Always need to buffer the namespaces regardless of what else happens so that XPath will work on attributes
    // namespace qualified attributes.
    bufferNamespaces( event, element );
    return element;
  }

  private void bufferNamespaces( StartElement event, Element element ) {
    Iterator namespaces = event.getNamespaces();
    while( namespaces.hasNext() ) {
      Namespace namespace = (Namespace)namespaces.next();
      if( namespace.isDefaultNamespaceDeclaration() ) {
        element.setAttribute( "xmlns", namespace.getNamespaceURI() );
      } else {
        element.setAttribute( "xmlns:" + namespace.getPrefix(), namespace.getNamespaceURI() );
      }
    }
  }

  private void streamElement( StartElement event, Element element ) throws XPathExpressionException {
    writer.write( "<" );
    QName qname = event.getName();
    String prefix = event.getName().getPrefix();
    if( prefix != null && !prefix.isEmpty() ) {
      writer.write( prefix );
      writer.write( ":" );
    }
    writer.write( qname.getLocalPart() );
    streamNamespaces( event );
    streamAttributes( event, element );
    writer.write( ">" );
  }

  private void processBufferedElement( Level level, UrlRewriteFilterGroupDescriptor config ) throws XPathExpressionException {
    for( UrlRewriteFilterPathDescriptor selector : config.getSelectors() ) {
      if( selector instanceof UrlRewriteFilterApplyDescriptor ) {
        XPathExpression path = (XPathExpression)selector.compiledPath( XPATH_COMPILER );
        Object node = path.evaluate( level.scopeNode, XPathConstants.NODE );
        if( node != null ) {
          UrlRewriteFilterApplyDescriptor apply = (UrlRewriteFilterApplyDescriptor)selector;
          if( node instanceof Element ) {
            Element element = (Element)node;
            String value = element.getTextContent();
            value = filterText( extractQName( element ), value, apply.rule() );
            element.setTextContent( value );
          } else if( node instanceof Text ) {
            Text text = (Text)node;
            String value = text.getWholeText();
            value = filterText( extractQName( text.getParentNode() ), value, apply.rule() );
            text.replaceWholeText( value );
          } else if( node instanceof Attr ) {
            Attr attr = (Attr)node;
            String value = attr.getValue();
            value = filterAttribute( extractQName( attr.getOwnerElement() ), extractQName( attr ), value, apply.rule() );
            attr.setValue( value );
          } else {
            throw new IllegalArgumentException( RES.unexpectedSelectedNodeType( node ) );
          }
        }
      } else if( selector instanceof UrlRewriteFilterDetectDescriptor ) {
        XPathExpression path = (XPathExpression)selector.compiledPath( XPATH_COMPILER );
        Object node = path.evaluate( level.scopeNode, XPathConstants.NODE );
        if( node != null ) {
          UrlRewriteFilterDetectDescriptor detect = (UrlRewriteFilterDetectDescriptor)selector;
          String value = null;
          if( node instanceof Element ) {
            Element element = (Element)node;
            value = element.getTextContent();
          } else if( node instanceof Text ) {
            Text text = (Text)node;
            value = text.getWholeText();
          } else if( node instanceof Attr ) {
            Attr attr = (Attr)node;
            value = attr.getValue();
          } else {
            throw new IllegalArgumentException( RES.unexpectedSelectedNodeType( node ) );
          }
          if( detect.compiledValue( REGEX_COMPILER ).matcher( value ).matches() ) {
            processBufferedElement( level, detect );
          }
        }
      } else {
        throw new IllegalArgumentException( RES.unexpectedRewritePathSelector( selector ) );
      }
    }
  }

  private void processBufferedElement( Level level ) throws XPathExpressionException, IOException {
    processBufferedElement( level, level.scopeConfig );
    writeBufferedElement( level.node, writer );
  }

  private QName extractQName( Node node ) {
    QName qname;
    String localName = node.getLocalName();
    if( localName == null ) {
      qname = new QName( node.getNodeName() );
    } else {
      qname = new QName( node.getNamespaceURI(), localName, node.getPrefix() );
    }
    return qname;
  }

  private void bufferAttributes( StartElement event, Element element ) {
    Iterator attributes = event.getAttributes();
    while( attributes.hasNext() ) {
      Attribute attribute = (Attribute)attributes.next();
      bufferAttribute( element, attribute );
    }
  }

  private Attr bufferAttribute( Element element, Attribute attribute ) {
    QName name = attribute.getName();
    String prefix = name.getPrefix();
    String uri = name.getNamespaceURI();
    Attr node;
    if( uri == null || uri.isEmpty() ) {
      node = document.createAttribute( name.getLocalPart() );
      element.setAttributeNode( node );
    } else {
      node = document.createAttributeNS( uri, name.getLocalPart() );
      if( prefix != null && !prefix.isEmpty() ) {
        node.setPrefix( prefix );
      }
      element.setAttributeNodeNS( node );
    }
    node.setTextContent( attribute.getValue() );
    return node;
  }

  private void streamNamespaces( StartElement event ) {
    Iterator i = event.getNamespaces();
    while( i.hasNext() ) {
      Namespace ns = (Namespace)i.next();
      if( ns.isDefaultNamespaceDeclaration() ) {
        writer.write( " xmlns=\"" );
      } else {
        writer.write( " xmlns:" );
        writer.write( ns.getPrefix() );
        writer.write( "=\"" );
        writer.write( ns.getNamespaceURI() );
      }
      writer.write( "\"" );
    }
  }

  private void streamAttributes( StartElement event, Element element ) throws XPathExpressionException {
    Iterator i = event.getAttributes();
    while( i.hasNext() ) {
      Attribute attribute = (Attribute)i.next();
      streamAttribute( element, attribute );
    }
  }

  private void streamAttribute( Element element, Attribute attribute ) throws XPathExpressionException {
    Attr node;
    QName name = attribute.getName();
    String prefix = name.getPrefix();
    String uri = name.getNamespaceURI();
    if( uri == null || uri.isEmpty() ) {
      node = document.createAttribute( name.getLocalPart() );
      element.setAttributeNode( node );
    } else {
      node = document.createAttributeNS( uri, name.getLocalPart() );
      if( prefix != null && !prefix.isEmpty() ) {
        node.setPrefix( prefix );
      }
      element.setAttributeNodeNS( node );
    }

    String value = attribute.getValue();
    Level level = stack.peek();
    if( ( level.scopeConfig ) == null || ( level.scopeConfig.getSelectors().isEmpty() ) ) {
      value = filterAttribute( null, attribute.getName(), value, null );
      node.setValue( value );
    } else {
      UrlRewriteFilterPathDescriptor path = pickFirstMatchingPath( level );
      if( path instanceof UrlRewriteFilterApplyDescriptor ) {
        String rule = ((UrlRewriteFilterApplyDescriptor)path).rule();
        value = filterAttribute( null, attribute.getName(), value, rule );
        node.setValue( value );
      }
    }

    //dump( document );

    if( prefix == null || prefix.isEmpty() ) {
      writer.write( " " );
      writer.write( name.getLocalPart() );
    } else {
      writer.write( " " );
      writer.write( prefix );
      writer.write( ":" );
      writer.write( name.getLocalPart() );
    }
    writer.write( "=\"" );
    writer.write( value );
    writer.write( "\"" );
    element.removeAttributeNode( node );
  }

  private void processCharacters( Characters event ) throws XPathExpressionException {
    //System.out.println( "T[" + event.isCData() + "," + event.isWhiteSpace() + "," + event.isIgnorableWhiteSpace() + "]=" + event );
    Level level = stack.peek();
    Node node = stack.peek().node;
    if( event.isCData() ) {
      node.appendChild( document.createCDATASection( event.getData() ) );
    } else {
      node.appendChild( document.createTextNode( event.getData() ) );
    }
    if( !currentlyBuffering() ) {
      String value = event.getData();
      if( !event.isWhiteSpace() ) {
        if( level.scopeConfig == null || level.scopeConfig.getSelectors().isEmpty() ) {
          value = filterText( extractQName( node ), value, null );
        } else {
          UrlRewriteFilterPathDescriptor path = pickFirstMatchingPath( level );
          if( path instanceof UrlRewriteFilterApplyDescriptor ) {
            String rule = ((UrlRewriteFilterApplyDescriptor)path).rule();
            value = filterText( extractQName( node ), value, rule );
          }
        }
      }
      writer.write( value );
    }
  }

  private void processComment( Comment event ) {
    //System.out.println( "C=" + event );
    if( currentlyBuffering() ) {
      stack.peek().node.appendChild( document.createComment( event.getText() ) );
    } else {
      writer.write( "<!--" );
      writer.write( event.getText() );
      writer.write( "-->" );
    }
  }

  @Override
  public void close() throws IOException {
    try {
      parser.close();
    } catch( XMLStreamException e ) {
      throw new IOException( e );
    }
    reader.close();
    writer.close();
    stack.clear();
  }

  protected UrlRewriteFilterPathDescriptor pickFirstMatchingPath( Level level ) {
    UrlRewriteFilterPathDescriptor match = null;
    if( level.scopeConfig != null ) {
      for( UrlRewriteFilterPathDescriptor selector : level.scopeConfig.getSelectors() ) {
        try {
          XPathExpression path = (XPathExpression)selector.compiledPath( XPATH_COMPILER );
          Object node = path.evaluate( level.scopeNode, XPathConstants.NODE );
          if( node != null ) {
            match = selector;
            break;
          }
        } catch( XPathExpressionException e ) {
          throw new IllegalArgumentException( selector.path(), e );
        }
      }
    }
    return match;
  }

  private boolean currentlyBuffering() {
    return stack.peek().buffered;
  }

  private Level pushLevel( Level parent, XMLEvent event, Node node, Node scopeNode, UrlRewriteFilterGroupDescriptor scopeConfig ) {
    Level level = new Level( parent, event, node, scopeNode, scopeConfig );
    stack.push( level );
    return level;
  }

  private static class Level {
//    private Level parent;
//    private XMLEvent event;
    private Node node;
    private UrlRewriteFilterGroupDescriptor scopeConfig;
    private Node scopeNode;
    private boolean buffered;

    private Level( Level parent, XMLEvent event, Node node, Node scopeNode, UrlRewriteFilterGroupDescriptor scopeConfig ) {
//      this.parent = parent;
//      this.event = event;
      this.node = node;
      this.scopeConfig = scopeConfig;
      this.scopeNode = scopeNode;
      this.buffered = ( ( parent != null ) && parent.buffered ) ||
                      ( ( scopeConfig != null ) && ( scopeConfig instanceof UrlRewriteFilterBufferDescriptor ) );
    }
  }

  private static class XmlPathCompiler implements UrlRewriteFilterPathDescriptor.Compiler<XPathExpression> {
    private static XPath XPATH = XPathFactory.newInstance().newXPath();
    @Override
    public XPathExpression compile( String expression, XPathExpression compiled ) {
      try {
        return XPATH.compile( expression );
      } catch( XPathExpressionException e ) {
        throw new IllegalArgumentException( e );
      }
    }
  }

  private static class RegexCompiler implements UrlRewriteFilterPathDescriptor.Compiler<Pattern> {
    @Override
    public Pattern compile( String expression, Pattern compiled ) {
      if( compiled != null ) {
        return compiled;
      } else {
        return Pattern.compile( expression );
      }
    }
  }

  private static final void writeBufferedElement( Node node, Writer writer ) throws IOException {
    try {
      getTransformer().transform( new DOMSource( node ), new StreamResult( writer ) );
    } catch( TransformerException e ) {
      throw new IOException( e );
    }
  }

  private static final Transformer getTransformer() throws TransformerConfigurationException {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer t = tf.newTransformer();
    t.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
    return t;
  }

}
