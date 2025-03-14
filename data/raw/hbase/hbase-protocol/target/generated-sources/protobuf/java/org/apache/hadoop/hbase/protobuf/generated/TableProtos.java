// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: Table.proto

package org.apache.hadoop.hbase.protobuf.generated;

@javax.annotation.Generated("proto") public final class TableProtos {
  private TableProtos() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface TableNameOrBuilder
      extends com.google.protobuf.MessageOrBuilder {

    // required bytes namespace = 1;
    /**
     * <code>required bytes namespace = 1;</code>
     */
    boolean hasNamespace();
    /**
     * <code>required bytes namespace = 1;</code>
     */
    com.google.protobuf.ByteString getNamespace();

    // required bytes qualifier = 2;
    /**
     * <code>required bytes qualifier = 2;</code>
     */
    boolean hasQualifier();
    /**
     * <code>required bytes qualifier = 2;</code>
     */
    com.google.protobuf.ByteString getQualifier();
  }
  /**
   * Protobuf type {@code hbase.pb.TableName}
   *
   * <pre>
   **
   * Table Name
   * </pre>
   */
  @javax.annotation.Generated("proto") public static final class TableName extends
      com.google.protobuf.GeneratedMessage
      implements TableNameOrBuilder {
    // Use TableName.newBuilder() to construct.
    private TableName(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private TableName(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final TableName defaultInstance;
    public static TableName getDefaultInstance() {
      return defaultInstance;
    }

    public TableName getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private TableName(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      initFields();
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              bitField0_ |= 0x00000001;
              namespace_ = input.readBytes();
              break;
            }
            case 18: {
              bitField0_ |= 0x00000002;
              qualifier_ = input.readBytes();
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e.getMessage()).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.apache.hadoop.hbase.protobuf.generated.TableProtos.internal_static_hbase_pb_TableName_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.apache.hadoop.hbase.protobuf.generated.TableProtos.internal_static_hbase_pb_TableName_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName.class, org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName.Builder.class);
    }

    public static com.google.protobuf.Parser<TableName> PARSER =
        new com.google.protobuf.AbstractParser<TableName>() {
      public TableName parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new TableName(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<TableName> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    // required bytes namespace = 1;
    public static final int NAMESPACE_FIELD_NUMBER = 1;
    private com.google.protobuf.ByteString namespace_;
    /**
     * <code>required bytes namespace = 1;</code>
     */
    public boolean hasNamespace() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required bytes namespace = 1;</code>
     */
    public com.google.protobuf.ByteString getNamespace() {
      return namespace_;
    }

    // required bytes qualifier = 2;
    public static final int QUALIFIER_FIELD_NUMBER = 2;
    private com.google.protobuf.ByteString qualifier_;
    /**
     * <code>required bytes qualifier = 2;</code>
     */
    public boolean hasQualifier() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required bytes qualifier = 2;</code>
     */
    public com.google.protobuf.ByteString getQualifier() {
      return qualifier_;
    }

    private void initFields() {
      namespace_ = com.google.protobuf.ByteString.EMPTY;
      qualifier_ = com.google.protobuf.ByteString.EMPTY;
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;

      if (!hasNamespace()) {
        memoizedIsInitialized = 0;
        return false;
      }
      if (!hasQualifier()) {
        memoizedIsInitialized = 0;
        return false;
      }
      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeBytes(1, namespace_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeBytes(2, qualifier_);
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, namespace_);
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(2, qualifier_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName)) {
        return super.equals(obj);
      }
      org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName other = (org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName) obj;

      boolean result = true;
      result = result && (hasNamespace() == other.hasNamespace());
      if (hasNamespace()) {
        result = result && getNamespace()
            .equals(other.getNamespace());
      }
      result = result && (hasQualifier() == other.hasQualifier());
      if (hasQualifier()) {
        result = result && getQualifier()
            .equals(other.getQualifier());
      }
      result = result &&
          getUnknownFields().equals(other.getUnknownFields());
      return result;
    }

    private int memoizedHashCode = 0;
    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptorForType().hashCode();
      if (hasNamespace()) {
        hash = (37 * hash) + NAMESPACE_FIELD_NUMBER;
        hash = (53 * hash) + getNamespace().hashCode();
      }
      if (hasQualifier()) {
        hash = (37 * hash) + QUALIFIER_FIELD_NUMBER;
        hash = (53 * hash) + getQualifier().hashCode();
      }
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code hbase.pb.TableName}
     *
     * <pre>
     **
     * Table Name
     * </pre>
     */
    @javax.annotation.Generated("proto") public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableNameOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return org.apache.hadoop.hbase.protobuf.generated.TableProtos.internal_static_hbase_pb_TableName_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return org.apache.hadoop.hbase.protobuf.generated.TableProtos.internal_static_hbase_pb_TableName_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName.class, org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName.Builder.class);
      }

      // Construct using org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        namespace_ = com.google.protobuf.ByteString.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000001);
        qualifier_ = com.google.protobuf.ByteString.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000002);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return org.apache.hadoop.hbase.protobuf.generated.TableProtos.internal_static_hbase_pb_TableName_descriptor;
      }

      public org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName getDefaultInstanceForType() {
        return org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName.getDefaultInstance();
      }

      public org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName build() {
        org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName buildPartial() {
        org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName result = new org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.namespace_ = namespace_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.qualifier_ = qualifier_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName) {
          return mergeFrom((org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName other) {
        if (other == org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName.getDefaultInstance()) return this;
        if (other.hasNamespace()) {
          setNamespace(other.getNamespace());
        }
        if (other.hasQualifier()) {
          setQualifier(other.getQualifier());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        if (!hasNamespace()) {
          
          return false;
        }
        if (!hasQualifier()) {
          
          return false;
        }
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (org.apache.hadoop.hbase.protobuf.generated.TableProtos.TableName) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      // required bytes namespace = 1;
      private com.google.protobuf.ByteString namespace_ = com.google.protobuf.ByteString.EMPTY;
      /**
       * <code>required bytes namespace = 1;</code>
       */
      public boolean hasNamespace() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>required bytes namespace = 1;</code>
       */
      public com.google.protobuf.ByteString getNamespace() {
        return namespace_;
      }
      /**
       * <code>required bytes namespace = 1;</code>
       */
      public Builder setNamespace(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        namespace_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required bytes namespace = 1;</code>
       */
      public Builder clearNamespace() {
        bitField0_ = (bitField0_ & ~0x00000001);
        namespace_ = getDefaultInstance().getNamespace();
        onChanged();
        return this;
      }

      // required bytes qualifier = 2;
      private com.google.protobuf.ByteString qualifier_ = com.google.protobuf.ByteString.EMPTY;
      /**
       * <code>required bytes qualifier = 2;</code>
       */
      public boolean hasQualifier() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>required bytes qualifier = 2;</code>
       */
      public com.google.protobuf.ByteString getQualifier() {
        return qualifier_;
      }
      /**
       * <code>required bytes qualifier = 2;</code>
       */
      public Builder setQualifier(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000002;
        qualifier_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>required bytes qualifier = 2;</code>
       */
      public Builder clearQualifier() {
        bitField0_ = (bitField0_ & ~0x00000002);
        qualifier_ = getDefaultInstance().getQualifier();
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:hbase.pb.TableName)
    }

    static {
      defaultInstance = new TableName(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:hbase.pb.TableName)
  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_hbase_pb_TableName_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_hbase_pb_TableName_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\013Table.proto\022\010hbase.pb\"1\n\tTableName\022\021\n\t" +
      "namespace\030\001 \002(\014\022\021\n\tqualifier\030\002 \002(\014B>\n*or" +
      "g.apache.hadoop.hbase.protobuf.generated" +
      "B\013TableProtosH\001\240\001\001"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_hbase_pb_TableName_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_hbase_pb_TableName_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_hbase_pb_TableName_descriptor,
              new java.lang.String[] { "Namespace", "Qualifier", });
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
