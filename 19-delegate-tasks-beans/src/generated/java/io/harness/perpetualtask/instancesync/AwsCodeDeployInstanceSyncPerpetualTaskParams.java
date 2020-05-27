// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/instancesync/aws_code_deploy_instance_sync_perpetual_task_params.proto

package io.harness.perpetualtask.instancesync;

/**
 * Protobuf type {@code io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams}
 */
@javax.annotation.
Generated(value = "protoc", comments = "annotations:AwsCodeDeployInstanceSyncPerpetualTaskParams.java.pb.meta")
public final class AwsCodeDeployInstanceSyncPerpetualTaskParams extends com.google.protobuf
                                                                            .GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams)
    AwsCodeDeployInstanceSyncPerpetualTaskParamsOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use AwsCodeDeployInstanceSyncPerpetualTaskParams.newBuilder() to construct.
  private AwsCodeDeployInstanceSyncPerpetualTaskParams(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private AwsCodeDeployInstanceSyncPerpetualTaskParams() {
    region_ = "";
    filter_ = com.google.protobuf.ByteString.EMPTY;
    awsConfig_ = com.google.protobuf.ByteString.EMPTY;
    encryptedData_ = com.google.protobuf.ByteString.EMPTY;
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private AwsCodeDeployInstanceSyncPerpetualTaskParams(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            region_ = s;
            break;
          }
          case 18: {
            filter_ = input.readBytes();
            break;
          }
          case 26: {
            awsConfig_ = input.readBytes();
            break;
          }
          case 34: {
            encryptedData_ = input.readBytes();
            break;
          }
          default: {
            if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParamsOuterClass
        .internal_static_io_harness_perpetualtask_instancesync_AwsCodeDeployInstanceSyncPerpetualTaskParams_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParamsOuterClass
        .internal_static_io_harness_perpetualtask_instancesync_AwsCodeDeployInstanceSyncPerpetualTaskParams_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams.class,
            io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams.Builder.class);
  }

  public static final int REGION_FIELD_NUMBER = 1;
  private volatile java.lang.Object region_;
  /**
   * <code>string region = 1;</code>
   */
  public java.lang.String getRegion() {
    java.lang.Object ref = region_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      region_ = s;
      return s;
    }
  }
  /**
   * <code>string region = 1;</code>
   */
  public com.google.protobuf.ByteString getRegionBytes() {
    java.lang.Object ref = region_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      region_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int FILTER_FIELD_NUMBER = 2;
  private com.google.protobuf.ByteString filter_;
  /**
   * <code>bytes filter = 2;</code>
   */
  public com.google.protobuf.ByteString getFilter() {
    return filter_;
  }

  public static final int AWS_CONFIG_FIELD_NUMBER = 3;
  private com.google.protobuf.ByteString awsConfig_;
  /**
   * <code>bytes aws_config = 3;</code>
   */
  public com.google.protobuf.ByteString getAwsConfig() {
    return awsConfig_;
  }

  public static final int ENCRYPTED_DATA_FIELD_NUMBER = 4;
  private com.google.protobuf.ByteString encryptedData_;
  /**
   * <code>bytes encrypted_data = 4;</code>
   */
  public com.google.protobuf.ByteString getEncryptedData() {
    return encryptedData_;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1)
      return true;
    if (isInitialized == 0)
      return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    if (!getRegionBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, region_);
    }
    if (!filter_.isEmpty()) {
      output.writeBytes(2, filter_);
    }
    if (!awsConfig_.isEmpty()) {
      output.writeBytes(3, awsConfig_);
    }
    if (!encryptedData_.isEmpty()) {
      output.writeBytes(4, encryptedData_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getRegionBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, region_);
    }
    if (!filter_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream.computeBytesSize(2, filter_);
    }
    if (!awsConfig_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream.computeBytesSize(3, awsConfig_);
    }
    if (!encryptedData_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream.computeBytesSize(4, encryptedData_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams)) {
      return super.equals(obj);
    }
    io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams other =
        (io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams) obj;

    if (!getRegion().equals(other.getRegion()))
      return false;
    if (!getFilter().equals(other.getFilter()))
      return false;
    if (!getAwsConfig().equals(other.getAwsConfig()))
      return false;
    if (!getEncryptedData().equals(other.getEncryptedData()))
      return false;
    if (!unknownFields.equals(other.unknownFields))
      return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + REGION_FIELD_NUMBER;
    hash = (53 * hash) + getRegion().hashCode();
    hash = (37 * hash) + FILTER_FIELD_NUMBER;
    hash = (53 * hash) + getFilter().hashCode();
    hash = (37 * hash) + AWS_CONFIG_FIELD_NUMBER;
    hash = (53 * hash) + getAwsConfig().hashCode();
    hash = (37 * hash) + ENCRYPTED_DATA_FIELD_NUMBER;
    hash = (53 * hash) + getEncryptedData().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      byte[] data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parseFrom(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() {
    return newBuilder();
  }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(
      io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams)
      io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParamsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParamsOuterClass
          .internal_static_io_harness_perpetualtask_instancesync_AwsCodeDeployInstanceSyncPerpetualTaskParams_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParamsOuterClass
          .internal_static_io_harness_perpetualtask_instancesync_AwsCodeDeployInstanceSyncPerpetualTaskParams_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams.class,
              io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams.Builder.class);
    }

    // Construct using io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      region_ = "";

      filter_ = com.google.protobuf.ByteString.EMPTY;

      awsConfig_ = com.google.protobuf.ByteString.EMPTY;

      encryptedData_ = com.google.protobuf.ByteString.EMPTY;

      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParamsOuterClass
          .internal_static_io_harness_perpetualtask_instancesync_AwsCodeDeployInstanceSyncPerpetualTaskParams_descriptor;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams
    getDefaultInstanceForType() {
      return io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams build() {
      io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams buildPartial() {
      io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams result =
          new io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams(this);
      result.region_ = region_;
      result.filter_ = filter_;
      result.awsConfig_ = awsConfig_;
      result.encryptedData_ = encryptedData_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field, int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams) {
        return mergeFrom((io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams other) {
      if (other
          == io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams.getDefaultInstance())
        return this;
      if (!other.getRegion().isEmpty()) {
        region_ = other.region_;
        onChanged();
      }
      if (other.getFilter() != com.google.protobuf.ByteString.EMPTY) {
        setFilter(other.getFilter());
      }
      if (other.getAwsConfig() != com.google.protobuf.ByteString.EMPTY) {
        setAwsConfig(other.getAwsConfig());
      }
      if (other.getEncryptedData() != com.google.protobuf.ByteString.EMPTY) {
        setEncryptedData(other.getEncryptedData());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
      io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams)
                            e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object region_ = "";
    /**
     * <code>string region = 1;</code>
     */
    public java.lang.String getRegion() {
      java.lang.Object ref = region_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        region_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string region = 1;</code>
     */
    public com.google.protobuf.ByteString getRegionBytes() {
      java.lang.Object ref = region_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        region_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string region = 1;</code>
     */
    public Builder setRegion(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      region_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string region = 1;</code>
     */
    public Builder clearRegion() {
      region_ = getDefaultInstance().getRegion();
      onChanged();
      return this;
    }
    /**
     * <code>string region = 1;</code>
     */
    public Builder setRegionBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      region_ = value;
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString filter_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <code>bytes filter = 2;</code>
     */
    public com.google.protobuf.ByteString getFilter() {
      return filter_;
    }
    /**
     * <code>bytes filter = 2;</code>
     */
    public Builder setFilter(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }

      filter_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bytes filter = 2;</code>
     */
    public Builder clearFilter() {
      filter_ = getDefaultInstance().getFilter();
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString awsConfig_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <code>bytes aws_config = 3;</code>
     */
    public com.google.protobuf.ByteString getAwsConfig() {
      return awsConfig_;
    }
    /**
     * <code>bytes aws_config = 3;</code>
     */
    public Builder setAwsConfig(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }

      awsConfig_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bytes aws_config = 3;</code>
     */
    public Builder clearAwsConfig() {
      awsConfig_ = getDefaultInstance().getAwsConfig();
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString encryptedData_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <code>bytes encrypted_data = 4;</code>
     */
    public com.google.protobuf.ByteString getEncryptedData() {
      return encryptedData_;
    }
    /**
     * <code>bytes encrypted_data = 4;</code>
     */
    public Builder setEncryptedData(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }

      encryptedData_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bytes encrypted_data = 4;</code>
     */
    public Builder clearEncryptedData() {
      encryptedData_ = getDefaultInstance().getEncryptedData();
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams)
  }

  // @@protoc_insertion_point(class_scope:io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams)
  private static final io.harness.perpetualtask.instancesync
      .AwsCodeDeployInstanceSyncPerpetualTaskParams DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams();
  }

  public static io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams
  getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<AwsCodeDeployInstanceSyncPerpetualTaskParams> PARSER =
      new com.google.protobuf.AbstractParser<AwsCodeDeployInstanceSyncPerpetualTaskParams>() {
        @java.lang.Override
        public AwsCodeDeployInstanceSyncPerpetualTaskParams parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new AwsCodeDeployInstanceSyncPerpetualTaskParams(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<AwsCodeDeployInstanceSyncPerpetualTaskParams> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<AwsCodeDeployInstanceSyncPerpetualTaskParams> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams
  getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
