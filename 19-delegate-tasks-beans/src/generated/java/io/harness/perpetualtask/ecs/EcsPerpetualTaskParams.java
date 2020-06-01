// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/ecs/ecs_task.proto

package io.harness.perpetualtask.ecs;

/**
 * Protobuf type {@code io.harness.perpetualtask.ecs.EcsPerpetualTaskParams}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:EcsPerpetualTaskParams.java.pb.meta")
public final class EcsPerpetualTaskParams extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.perpetualtask.ecs.EcsPerpetualTaskParams)
    EcsPerpetualTaskParamsOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use EcsPerpetualTaskParams.newBuilder() to construct.
  private EcsPerpetualTaskParams(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private EcsPerpetualTaskParams() {
    clusterName_ = "";
    region_ = "";
    awsConfig_ = com.google.protobuf.ByteString.EMPTY;
    encryptionDetail_ = com.google.protobuf.ByteString.EMPTY;
    clusterId_ = "";
    settingId_ = "";
  }

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new EcsPerpetualTaskParams();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private EcsPerpetualTaskParams(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
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

            clusterName_ = s;
            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();

            region_ = s;
            break;
          }
          case 26: {
            awsConfig_ = input.readBytes();
            break;
          }
          case 34: {
            encryptionDetail_ = input.readBytes();
            break;
          }
          case 42: {
            java.lang.String s = input.readStringRequireUtf8();

            clusterId_ = s;
            break;
          }
          case 50: {
            java.lang.String s = input.readStringRequireUtf8();

            settingId_ = s;
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
    return io.harness.perpetualtask.ecs.EcsTask
        .internal_static_io_harness_perpetualtask_ecs_EcsPerpetualTaskParams_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.perpetualtask.ecs.EcsTask
        .internal_static_io_harness_perpetualtask_ecs_EcsPerpetualTaskParams_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.perpetualtask.ecs.EcsPerpetualTaskParams.class,
            io.harness.perpetualtask.ecs.EcsPerpetualTaskParams.Builder.class);
  }

  public static final int CLUSTER_NAME_FIELD_NUMBER = 1;
  private volatile java.lang.Object clusterName_;
  /**
   * <code>string cluster_name = 1[json_name = "clusterName"];</code>
   * @return The clusterName.
   */
  public java.lang.String getClusterName() {
    java.lang.Object ref = clusterName_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      clusterName_ = s;
      return s;
    }
  }
  /**
   * <code>string cluster_name = 1[json_name = "clusterName"];</code>
   * @return The bytes for clusterName.
   */
  public com.google.protobuf.ByteString getClusterNameBytes() {
    java.lang.Object ref = clusterName_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      clusterName_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int REGION_FIELD_NUMBER = 2;
  private volatile java.lang.Object region_;
  /**
   * <code>string region = 2[json_name = "region"];</code>
   * @return The region.
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
   * <code>string region = 2[json_name = "region"];</code>
   * @return The bytes for region.
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

  public static final int AWS_CONFIG_FIELD_NUMBER = 3;
  private com.google.protobuf.ByteString awsConfig_;
  /**
   * <code>bytes aws_config = 3[json_name = "awsConfig"];</code>
   * @return The awsConfig.
   */
  public com.google.protobuf.ByteString getAwsConfig() {
    return awsConfig_;
  }

  public static final int ENCRYPTION_DETAIL_FIELD_NUMBER = 4;
  private com.google.protobuf.ByteString encryptionDetail_;
  /**
   * <code>bytes encryption_detail = 4[json_name = "encryptionDetail"];</code>
   * @return The encryptionDetail.
   */
  public com.google.protobuf.ByteString getEncryptionDetail() {
    return encryptionDetail_;
  }

  public static final int CLUSTER_ID_FIELD_NUMBER = 5;
  private volatile java.lang.Object clusterId_;
  /**
   * <code>string cluster_id = 5[json_name = "clusterId"];</code>
   * @return The clusterId.
   */
  public java.lang.String getClusterId() {
    java.lang.Object ref = clusterId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      clusterId_ = s;
      return s;
    }
  }
  /**
   * <code>string cluster_id = 5[json_name = "clusterId"];</code>
   * @return The bytes for clusterId.
   */
  public com.google.protobuf.ByteString getClusterIdBytes() {
    java.lang.Object ref = clusterId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      clusterId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int SETTING_ID_FIELD_NUMBER = 6;
  private volatile java.lang.Object settingId_;
  /**
   * <code>string setting_id = 6[json_name = "settingId"];</code>
   * @return The settingId.
   */
  public java.lang.String getSettingId() {
    java.lang.Object ref = settingId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      settingId_ = s;
      return s;
    }
  }
  /**
   * <code>string setting_id = 6[json_name = "settingId"];</code>
   * @return The bytes for settingId.
   */
  public com.google.protobuf.ByteString getSettingIdBytes() {
    java.lang.Object ref = settingId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      settingId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
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
    if (!getClusterNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, clusterName_);
    }
    if (!getRegionBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, region_);
    }
    if (!awsConfig_.isEmpty()) {
      output.writeBytes(3, awsConfig_);
    }
    if (!encryptionDetail_.isEmpty()) {
      output.writeBytes(4, encryptionDetail_);
    }
    if (!getClusterIdBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 5, clusterId_);
    }
    if (!getSettingIdBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 6, settingId_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getClusterNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, clusterName_);
    }
    if (!getRegionBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, region_);
    }
    if (!awsConfig_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream.computeBytesSize(3, awsConfig_);
    }
    if (!encryptionDetail_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream.computeBytesSize(4, encryptionDetail_);
    }
    if (!getClusterIdBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(5, clusterId_);
    }
    if (!getSettingIdBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(6, settingId_);
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
    if (!(obj instanceof io.harness.perpetualtask.ecs.EcsPerpetualTaskParams)) {
      return super.equals(obj);
    }
    io.harness.perpetualtask.ecs.EcsPerpetualTaskParams other =
        (io.harness.perpetualtask.ecs.EcsPerpetualTaskParams) obj;

    if (!getClusterName().equals(other.getClusterName()))
      return false;
    if (!getRegion().equals(other.getRegion()))
      return false;
    if (!getAwsConfig().equals(other.getAwsConfig()))
      return false;
    if (!getEncryptionDetail().equals(other.getEncryptionDetail()))
      return false;
    if (!getClusterId().equals(other.getClusterId()))
      return false;
    if (!getSettingId().equals(other.getSettingId()))
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
    hash = (37 * hash) + CLUSTER_NAME_FIELD_NUMBER;
    hash = (53 * hash) + getClusterName().hashCode();
    hash = (37 * hash) + REGION_FIELD_NUMBER;
    hash = (53 * hash) + getRegion().hashCode();
    hash = (37 * hash) + AWS_CONFIG_FIELD_NUMBER;
    hash = (53 * hash) + getAwsConfig().hashCode();
    hash = (37 * hash) + ENCRYPTION_DETAIL_FIELD_NUMBER;
    hash = (53 * hash) + getEncryptionDetail().hashCode();
    hash = (37 * hash) + CLUSTER_ID_FIELD_NUMBER;
    hash = (53 * hash) + getClusterId().hashCode();
    hash = (37 * hash) + SETTING_ID_FIELD_NUMBER;
    hash = (53 * hash) + getSettingId().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parseFrom(
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
  public static Builder newBuilder(io.harness.perpetualtask.ecs.EcsPerpetualTaskParams prototype) {
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
   * Protobuf type {@code io.harness.perpetualtask.ecs.EcsPerpetualTaskParams}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.perpetualtask.ecs.EcsPerpetualTaskParams)
      io.harness.perpetualtask.ecs.EcsPerpetualTaskParamsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.perpetualtask.ecs.EcsTask
          .internal_static_io_harness_perpetualtask_ecs_EcsPerpetualTaskParams_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.perpetualtask.ecs.EcsTask
          .internal_static_io_harness_perpetualtask_ecs_EcsPerpetualTaskParams_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.perpetualtask.ecs.EcsPerpetualTaskParams.class,
              io.harness.perpetualtask.ecs.EcsPerpetualTaskParams.Builder.class);
    }

    // Construct using io.harness.perpetualtask.ecs.EcsPerpetualTaskParams.newBuilder()
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
      clusterName_ = "";

      region_ = "";

      awsConfig_ = com.google.protobuf.ByteString.EMPTY;

      encryptionDetail_ = com.google.protobuf.ByteString.EMPTY;

      clusterId_ = "";

      settingId_ = "";

      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.perpetualtask.ecs.EcsTask
          .internal_static_io_harness_perpetualtask_ecs_EcsPerpetualTaskParams_descriptor;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.ecs.EcsPerpetualTaskParams getDefaultInstanceForType() {
      return io.harness.perpetualtask.ecs.EcsPerpetualTaskParams.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.ecs.EcsPerpetualTaskParams build() {
      io.harness.perpetualtask.ecs.EcsPerpetualTaskParams result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.ecs.EcsPerpetualTaskParams buildPartial() {
      io.harness.perpetualtask.ecs.EcsPerpetualTaskParams result =
          new io.harness.perpetualtask.ecs.EcsPerpetualTaskParams(this);
      result.clusterName_ = clusterName_;
      result.region_ = region_;
      result.awsConfig_ = awsConfig_;
      result.encryptionDetail_ = encryptionDetail_;
      result.clusterId_ = clusterId_;
      result.settingId_ = settingId_;
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
      if (other instanceof io.harness.perpetualtask.ecs.EcsPerpetualTaskParams) {
        return mergeFrom((io.harness.perpetualtask.ecs.EcsPerpetualTaskParams) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.perpetualtask.ecs.EcsPerpetualTaskParams other) {
      if (other == io.harness.perpetualtask.ecs.EcsPerpetualTaskParams.getDefaultInstance())
        return this;
      if (!other.getClusterName().isEmpty()) {
        clusterName_ = other.clusterName_;
        onChanged();
      }
      if (!other.getRegion().isEmpty()) {
        region_ = other.region_;
        onChanged();
      }
      if (other.getAwsConfig() != com.google.protobuf.ByteString.EMPTY) {
        setAwsConfig(other.getAwsConfig());
      }
      if (other.getEncryptionDetail() != com.google.protobuf.ByteString.EMPTY) {
        setEncryptionDetail(other.getEncryptionDetail());
      }
      if (!other.getClusterId().isEmpty()) {
        clusterId_ = other.clusterId_;
        onChanged();
      }
      if (!other.getSettingId().isEmpty()) {
        settingId_ = other.settingId_;
        onChanged();
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
      io.harness.perpetualtask.ecs.EcsPerpetualTaskParams parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.perpetualtask.ecs.EcsPerpetualTaskParams) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object clusterName_ = "";
    /**
     * <code>string cluster_name = 1[json_name = "clusterName"];</code>
     * @return The clusterName.
     */
    public java.lang.String getClusterName() {
      java.lang.Object ref = clusterName_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        clusterName_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string cluster_name = 1[json_name = "clusterName"];</code>
     * @return The bytes for clusterName.
     */
    public com.google.protobuf.ByteString getClusterNameBytes() {
      java.lang.Object ref = clusterName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        clusterName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string cluster_name = 1[json_name = "clusterName"];</code>
     * @param value The clusterName to set.
     * @return This builder for chaining.
     */
    public Builder setClusterName(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      clusterName_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string cluster_name = 1[json_name = "clusterName"];</code>
     * @return This builder for chaining.
     */
    public Builder clearClusterName() {
      clusterName_ = getDefaultInstance().getClusterName();
      onChanged();
      return this;
    }
    /**
     * <code>string cluster_name = 1[json_name = "clusterName"];</code>
     * @param value The bytes for clusterName to set.
     * @return This builder for chaining.
     */
    public Builder setClusterNameBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      clusterName_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object region_ = "";
    /**
     * <code>string region = 2[json_name = "region"];</code>
     * @return The region.
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
     * <code>string region = 2[json_name = "region"];</code>
     * @return The bytes for region.
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
     * <code>string region = 2[json_name = "region"];</code>
     * @param value The region to set.
     * @return This builder for chaining.
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
     * <code>string region = 2[json_name = "region"];</code>
     * @return This builder for chaining.
     */
    public Builder clearRegion() {
      region_ = getDefaultInstance().getRegion();
      onChanged();
      return this;
    }
    /**
     * <code>string region = 2[json_name = "region"];</code>
     * @param value The bytes for region to set.
     * @return This builder for chaining.
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

    private com.google.protobuf.ByteString awsConfig_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <code>bytes aws_config = 3[json_name = "awsConfig"];</code>
     * @return The awsConfig.
     */
    public com.google.protobuf.ByteString getAwsConfig() {
      return awsConfig_;
    }
    /**
     * <code>bytes aws_config = 3[json_name = "awsConfig"];</code>
     * @param value The awsConfig to set.
     * @return This builder for chaining.
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
     * <code>bytes aws_config = 3[json_name = "awsConfig"];</code>
     * @return This builder for chaining.
     */
    public Builder clearAwsConfig() {
      awsConfig_ = getDefaultInstance().getAwsConfig();
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString encryptionDetail_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <code>bytes encryption_detail = 4[json_name = "encryptionDetail"];</code>
     * @return The encryptionDetail.
     */
    public com.google.protobuf.ByteString getEncryptionDetail() {
      return encryptionDetail_;
    }
    /**
     * <code>bytes encryption_detail = 4[json_name = "encryptionDetail"];</code>
     * @param value The encryptionDetail to set.
     * @return This builder for chaining.
     */
    public Builder setEncryptionDetail(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }

      encryptionDetail_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bytes encryption_detail = 4[json_name = "encryptionDetail"];</code>
     * @return This builder for chaining.
     */
    public Builder clearEncryptionDetail() {
      encryptionDetail_ = getDefaultInstance().getEncryptionDetail();
      onChanged();
      return this;
    }

    private java.lang.Object clusterId_ = "";
    /**
     * <code>string cluster_id = 5[json_name = "clusterId"];</code>
     * @return The clusterId.
     */
    public java.lang.String getClusterId() {
      java.lang.Object ref = clusterId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        clusterId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string cluster_id = 5[json_name = "clusterId"];</code>
     * @return The bytes for clusterId.
     */
    public com.google.protobuf.ByteString getClusterIdBytes() {
      java.lang.Object ref = clusterId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        clusterId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string cluster_id = 5[json_name = "clusterId"];</code>
     * @param value The clusterId to set.
     * @return This builder for chaining.
     */
    public Builder setClusterId(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      clusterId_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string cluster_id = 5[json_name = "clusterId"];</code>
     * @return This builder for chaining.
     */
    public Builder clearClusterId() {
      clusterId_ = getDefaultInstance().getClusterId();
      onChanged();
      return this;
    }
    /**
     * <code>string cluster_id = 5[json_name = "clusterId"];</code>
     * @param value The bytes for clusterId to set.
     * @return This builder for chaining.
     */
    public Builder setClusterIdBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      clusterId_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object settingId_ = "";
    /**
     * <code>string setting_id = 6[json_name = "settingId"];</code>
     * @return The settingId.
     */
    public java.lang.String getSettingId() {
      java.lang.Object ref = settingId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        settingId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string setting_id = 6[json_name = "settingId"];</code>
     * @return The bytes for settingId.
     */
    public com.google.protobuf.ByteString getSettingIdBytes() {
      java.lang.Object ref = settingId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        settingId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string setting_id = 6[json_name = "settingId"];</code>
     * @param value The settingId to set.
     * @return This builder for chaining.
     */
    public Builder setSettingId(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      settingId_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string setting_id = 6[json_name = "settingId"];</code>
     * @return This builder for chaining.
     */
    public Builder clearSettingId() {
      settingId_ = getDefaultInstance().getSettingId();
      onChanged();
      return this;
    }
    /**
     * <code>string setting_id = 6[json_name = "settingId"];</code>
     * @param value The bytes for settingId to set.
     * @return This builder for chaining.
     */
    public Builder setSettingIdBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      settingId_ = value;
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

    // @@protoc_insertion_point(builder_scope:io.harness.perpetualtask.ecs.EcsPerpetualTaskParams)
  }

  // @@protoc_insertion_point(class_scope:io.harness.perpetualtask.ecs.EcsPerpetualTaskParams)
  private static final io.harness.perpetualtask.ecs.EcsPerpetualTaskParams DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.perpetualtask.ecs.EcsPerpetualTaskParams();
  }

  public static io.harness.perpetualtask.ecs.EcsPerpetualTaskParams getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<EcsPerpetualTaskParams> PARSER =
      new com.google.protobuf.AbstractParser<EcsPerpetualTaskParams>() {
        @java.lang.Override
        public EcsPerpetualTaskParams parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new EcsPerpetualTaskParams(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<EcsPerpetualTaskParams> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<EcsPerpetualTaskParams> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.perpetualtask.ecs.EcsPerpetualTaskParams getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
