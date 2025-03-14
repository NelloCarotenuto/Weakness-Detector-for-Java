/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.camel.Processor;
import org.apache.camel.model.dataformat.ASN1DataFormat;
import org.apache.camel.model.dataformat.AvroDataFormat;
import org.apache.camel.model.dataformat.Base64DataFormat;
import org.apache.camel.model.dataformat.BeanioDataFormat;
import org.apache.camel.model.dataformat.BindyDataFormat;
import org.apache.camel.model.dataformat.BoonDataFormat;
import org.apache.camel.model.dataformat.CastorDataFormat;
import org.apache.camel.model.dataformat.CryptoDataFormat;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.model.dataformat.CustomDataFormat;
import org.apache.camel.model.dataformat.FlatpackDataFormat;
import org.apache.camel.model.dataformat.GzipDataFormat;
import org.apache.camel.model.dataformat.HL7DataFormat;
import org.apache.camel.model.dataformat.HessianDataFormat;
import org.apache.camel.model.dataformat.IcalDataFormat;
import org.apache.camel.model.dataformat.JacksonXMLDataFormat;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.apache.camel.model.dataformat.JibxDataFormat;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.PGPDataFormat;
import org.apache.camel.model.dataformat.ProtobufDataFormat;
import org.apache.camel.model.dataformat.RssDataFormat;
import org.apache.camel.model.dataformat.SerializationDataFormat;
import org.apache.camel.model.dataformat.SoapJaxbDataFormat;
import org.apache.camel.model.dataformat.StringDataFormat;
import org.apache.camel.model.dataformat.SyslogDataFormat;
import org.apache.camel.model.dataformat.TarFileDataFormat;
import org.apache.camel.model.dataformat.ThriftDataFormat;
import org.apache.camel.model.dataformat.TidyMarkupDataFormat;
import org.apache.camel.model.dataformat.UniVocityCsvDataFormat;
import org.apache.camel.model.dataformat.UniVocityFixedWidthDataFormat;
import org.apache.camel.model.dataformat.UniVocityTsvDataFormat;
import org.apache.camel.model.dataformat.XMLBeansDataFormat;
import org.apache.camel.model.dataformat.XMLSecurityDataFormat;
import org.apache.camel.model.dataformat.XStreamDataFormat;
import org.apache.camel.model.dataformat.XmlJsonDataFormat;
import org.apache.camel.model.dataformat.XmlRpcDataFormat;
import org.apache.camel.model.dataformat.YAMLDataFormat;
import org.apache.camel.model.dataformat.ZipDataFormat;
import org.apache.camel.model.dataformat.ZipFileDataFormat;
import org.apache.camel.processor.UnmarshalProcessor;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.RouteContext;

/**
 * Converts the message data received from the wire into a format that Apache Camel processors can consume
 *
 * @version
 */
@Metadata(label = "eip,transformation")
@XmlRootElement(name = "unmarshal")
@XmlAccessorType(XmlAccessType.FIELD)
public class UnmarshalDefinition extends NoOutputDefinition<UnmarshalDefinition> {

    // TODO: Camel 3.0, ref attribute should be removed as UnmarshalDataFormat is to be used instead

    // cannot use @XmlElementRef as it doesn't allow optional properties
    @XmlElements({
        @XmlElement(required = false, name = "asn1", type = ASN1DataFormat.class),
        @XmlElement(required = false, name = "avro", type = AvroDataFormat.class),
        @XmlElement(required = false, name = "base64", type = Base64DataFormat.class),
        @XmlElement(required = false, name = "beanio", type = BeanioDataFormat.class),
        @XmlElement(required = false, name = "bindy", type = BindyDataFormat.class),
        @XmlElement(required = false, name = "boon", type = BoonDataFormat.class),
        @XmlElement(required = false, name = "castor", type = CastorDataFormat.class),
        @XmlElement(required = false, name = "crypto", type = CryptoDataFormat.class),
        @XmlElement(required = false, name = "csv", type = CsvDataFormat.class),
        @XmlElement(required = false, name = "custom", type = CustomDataFormat.class),
        @XmlElement(required = false, name = "flatpack", type = FlatpackDataFormat.class),
        @XmlElement(required = false, name = "gzip", type = GzipDataFormat.class),
        @XmlElement(required = false, name = "hessian", type = HessianDataFormat.class),
        @XmlElement(required = false, name = "hl7", type = HL7DataFormat.class),
        @XmlElement(required = false, name = "ical", type = IcalDataFormat.class),
        @XmlElement(required = false, name = "jacksonxml", type = JacksonXMLDataFormat.class),
        @XmlElement(required = false, name = "jaxb", type = JaxbDataFormat.class),
        @XmlElement(required = false, name = "jibx", type = JibxDataFormat.class),
        @XmlElement(required = false, name = "json", type = JsonDataFormat.class),
        @XmlElement(required = false, name = "protobuf", type = ProtobufDataFormat.class),
        @XmlElement(required = false, name = "rss", type = RssDataFormat.class),
        @XmlElement(required = false, name = "secureXML", type = XMLSecurityDataFormat.class),
        @XmlElement(required = false, name = "serialization", type = SerializationDataFormat.class),
        @XmlElement(required = false, name = "soapjaxb", type = SoapJaxbDataFormat.class),
        @XmlElement(required = false, name = "string", type = StringDataFormat.class),
        @XmlElement(required = false, name = "syslog", type = SyslogDataFormat.class),
        @XmlElement(required = false, name = "tarfile", type = TarFileDataFormat.class),
        @XmlElement(required = false, name = "thrift", type = ThriftDataFormat.class),
        @XmlElement(required = false, name = "tidyMarkup", type = TidyMarkupDataFormat.class),
        @XmlElement(required = false, name = "univocity-csv", type = UniVocityCsvDataFormat.class),
        @XmlElement(required = false, name = "univocity-fixed", type = UniVocityFixedWidthDataFormat.class),
        @XmlElement(required = false, name = "univocity-tsv", type = UniVocityTsvDataFormat.class),
        @XmlElement(required = false, name = "xmlBeans", type = XMLBeansDataFormat.class),
        @XmlElement(required = false, name = "xmljson", type = XmlJsonDataFormat.class),
        @XmlElement(required = false, name = "xmlrpc", type = XmlRpcDataFormat.class),
        @XmlElement(required = false, name = "xstream", type = XStreamDataFormat.class),
        @XmlElement(required = false, name = "pgp", type = PGPDataFormat.class),
        @XmlElement(required = false, name = "yaml", type = YAMLDataFormat.class),
        @XmlElement(required = false, name = "zip", type = ZipDataFormat.class),
        @XmlElement(required = false, name = "zipFile", type = ZipFileDataFormat.class)}
        )
    private DataFormatDefinition dataFormatType;

    @XmlAttribute
    @Deprecated
    private String ref;

    public UnmarshalDefinition() {
    }

    public UnmarshalDefinition(DataFormatDefinition dataFormatType) {
        this.dataFormatType = dataFormatType;
    }

    public UnmarshalDefinition(String ref) {
        this.ref = ref;
    }

    @Override
    public String toString() {
        return "Unmarshal[" + description() + "]";
    }

    protected String description() {
        if (dataFormatType != null) {
            return dataFormatType.toString();
        } else {
            return "ref:" + ref;
        }
    }

    @Override
    public String getLabel() {
        return "unmarshal[" + description() + "]";
    }

    public String getRef() {
        return ref;
    }

    /**
     * To refer to a custom data format to use as unmarshaller
     *
     * @deprecated use custom dataformat instead
     */
    @Deprecated
    public void setRef(String ref) {
        this.ref = ref;
    }

    public DataFormatDefinition getDataFormatType() {
        return dataFormatType;
    }

    /**
     * The data format to be used
     */
    public void setDataFormatType(DataFormatDefinition dataFormatType) {
        this.dataFormatType = dataFormatType;
    }

    @Override
    public Processor createProcessor(RouteContext routeContext) {
        DataFormat dataFormat = DataFormatDefinition.getDataFormat(routeContext, getDataFormatType(), ref);
        return new UnmarshalProcessor(dataFormat);
    }
}
