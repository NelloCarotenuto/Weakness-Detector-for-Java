// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: hellocamel.proto

package org.apache.camel.examples;

public final class HelloCamelProto {
  private HelloCamelProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_camel_examples_CamelHelloRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_camel_examples_CamelHelloRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_org_apache_camel_examples_CamelHelloReply_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_camel_examples_CamelHelloReply_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020hellocamel.proto\022\031org.apache.camel.exa" +
      "mples\"!\n\021CamelHelloRequest\022\014\n\004name\030\001 \001(\t" +
      "\"\"\n\017CamelHelloReply\022\017\n\007message\030\001 \001(\t2{\n\n" +
      "CamelHello\022m\n\017SayHelloToCamel\022,.org.apac" +
      "he.camel.examples.CamelHelloRequest\032*.or" +
      "g.apache.camel.examples.CamelHelloReply\"" +
      "\000B.\n\031org.apache.camel.examplesB\017HelloCam" +
      "elProtoP\001b\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_org_apache_camel_examples_CamelHelloRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_apache_camel_examples_CamelHelloRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_camel_examples_CamelHelloRequest_descriptor,
        new java.lang.String[] { "Name", });
    internal_static_org_apache_camel_examples_CamelHelloReply_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_org_apache_camel_examples_CamelHelloReply_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_camel_examples_CamelHelloReply_descriptor,
        new java.lang.String[] { "Message", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
