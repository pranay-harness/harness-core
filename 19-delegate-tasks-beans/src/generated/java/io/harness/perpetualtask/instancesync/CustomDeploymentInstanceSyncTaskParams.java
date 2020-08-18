// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/instancesync/custom_deployment_perpetual_task_params.proto

package io.harness.perpetualtask.instancesync;

/**
 * Protobuf type {@code io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams}
 */
@javax.annotation.
Generated(value = "protoc", comments = "annotations:CustomDeploymentInstanceSyncTaskParams.java.pb.meta")
public final class CustomDeploymentInstanceSyncTaskParams extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams)
    CustomDeploymentInstanceSyncTaskParamsOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use CustomDeploymentInstanceSyncTaskParams.newBuilder() to construct.
  private CustomDeploymentInstanceSyncTaskParams(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private CustomDeploymentInstanceSyncTaskParams() {
    appId_ = "";
    accountId_ = "";
    script_ = "";
    outputPathKey_ = "";
  }

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new CustomDeploymentInstanceSyncTaskParams();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private CustomDeploymentInstanceSyncTaskParams(
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

            appId_ = s;
            break;
          }
          case 18: {
            java.lang.String s = input.readStringRequireUtf8();

            accountId_ = s;
            break;
          }
          case 26: {
            java.lang.String s = input.readStringRequireUtf8();

            script_ = s;
            break;
          }
          case 34: {
            java.lang.String s = input.readStringRequireUtf8();

            outputPathKey_ = s;
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
    return io.harness.perpetualtask.instancesync.CustomDeploymentPerpetualTaskParams
        .internal_static_io_harness_perpetualtask_instancesync_CustomDeploymentInstanceSyncTaskParams_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.perpetualtask.instancesync.CustomDeploymentPerpetualTaskParams
        .internal_static_io_harness_perpetualtask_instancesync_CustomDeploymentInstanceSyncTaskParams_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams.class,
            io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams.Builder.class);
  }

  public static final int APP_ID_FIELD_NUMBER = 1;
  private volatile java.lang.Object appId_;
  /**
   * <code>string app_id = 1[json_name = "appId"];</code>
   * @return The appId.
   */
  public java.lang.String getAppId() {
    java.lang.Object ref = appId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      appId_ = s;
      return s;
    }
  }
  /**
   * <code>string app_id = 1[json_name = "appId"];</code>
   * @return The bytes for appId.
   */
  public com.google.protobuf.ByteString getAppIdBytes() {
    java.lang.Object ref = appId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      appId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int ACCOUNT_ID_FIELD_NUMBER = 2;
  private volatile java.lang.Object accountId_;
  /**
   * <code>string account_id = 2[json_name = "accountId"];</code>
   * @return The accountId.
   */
  public java.lang.String getAccountId() {
    java.lang.Object ref = accountId_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      accountId_ = s;
      return s;
    }
  }
  /**
   * <code>string account_id = 2[json_name = "accountId"];</code>
   * @return The bytes for accountId.
   */
  public com.google.protobuf.ByteString getAccountIdBytes() {
    java.lang.Object ref = accountId_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      accountId_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int SCRIPT_FIELD_NUMBER = 3;
  private volatile java.lang.Object script_;
  /**
   * <code>string script = 3[json_name = "script"];</code>
   * @return The script.
   */
  public java.lang.String getScript() {
    java.lang.Object ref = script_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      script_ = s;
      return s;
    }
  }
  /**
   * <code>string script = 3[json_name = "script"];</code>
   * @return The bytes for script.
   */
  public com.google.protobuf.ByteString getScriptBytes() {
    java.lang.Object ref = script_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      script_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int OUTPUT_PATH_KEY_FIELD_NUMBER = 4;
  private volatile java.lang.Object outputPathKey_;
  /**
   * <code>string output_path_key = 4[json_name = "outputPathKey"];</code>
   * @return The outputPathKey.
   */
  public java.lang.String getOutputPathKey() {
    java.lang.Object ref = outputPathKey_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      outputPathKey_ = s;
      return s;
    }
  }
  /**
   * <code>string output_path_key = 4[json_name = "outputPathKey"];</code>
   * @return The bytes for outputPathKey.
   */
  public com.google.protobuf.ByteString getOutputPathKeyBytes() {
    java.lang.Object ref = outputPathKey_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      outputPathKey_ = b;
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
    if (!getAppIdBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, appId_);
    }
    if (!getAccountIdBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, accountId_);
    }
    if (!getScriptBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 3, script_);
    }
    if (!getOutputPathKeyBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, outputPathKey_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getAppIdBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, appId_);
    }
    if (!getAccountIdBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, accountId_);
    }
    if (!getScriptBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, script_);
    }
    if (!getOutputPathKeyBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, outputPathKey_);
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
    if (!(obj instanceof io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams)) {
      return super.equals(obj);
    }
    io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams other =
        (io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams) obj;

    if (!getAppId().equals(other.getAppId()))
      return false;
    if (!getAccountId().equals(other.getAccountId()))
      return false;
    if (!getScript().equals(other.getScript()))
      return false;
    if (!getOutputPathKey().equals(other.getOutputPathKey()))
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
    hash = (37 * hash) + APP_ID_FIELD_NUMBER;
    hash = (53 * hash) + getAppId().hashCode();
    hash = (37 * hash) + ACCOUNT_ID_FIELD_NUMBER;
    hash = (53 * hash) + getAccountId().hashCode();
    hash = (37 * hash) + SCRIPT_FIELD_NUMBER;
    hash = (53 * hash) + getScript().hashCode();
    hash = (37 * hash) + OUTPUT_PATH_KEY_FIELD_NUMBER;
    hash = (53 * hash) + getOutputPathKey().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
      java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
      com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parseFrom(
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
      io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams prototype) {
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
   * Protobuf type {@code io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams)
      io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParamsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.perpetualtask.instancesync.CustomDeploymentPerpetualTaskParams
          .internal_static_io_harness_perpetualtask_instancesync_CustomDeploymentInstanceSyncTaskParams_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.perpetualtask.instancesync.CustomDeploymentPerpetualTaskParams
          .internal_static_io_harness_perpetualtask_instancesync_CustomDeploymentInstanceSyncTaskParams_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams.class,
              io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams.Builder.class);
    }

    // Construct using io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams.newBuilder()
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
      appId_ = "";

      accountId_ = "";

      script_ = "";

      outputPathKey_ = "";

      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.perpetualtask.instancesync.CustomDeploymentPerpetualTaskParams
          .internal_static_io_harness_perpetualtask_instancesync_CustomDeploymentInstanceSyncTaskParams_descriptor;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams getDefaultInstanceForType() {
      return io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams build() {
      io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams buildPartial() {
      io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams result =
          new io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams(this);
      result.appId_ = appId_;
      result.accountId_ = accountId_;
      result.script_ = script_;
      result.outputPathKey_ = outputPathKey_;
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
      if (other instanceof io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams) {
        return mergeFrom((io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams other) {
      if (other == io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams.getDefaultInstance())
        return this;
      if (!other.getAppId().isEmpty()) {
        appId_ = other.appId_;
        onChanged();
      }
      if (!other.getAccountId().isEmpty()) {
        accountId_ = other.accountId_;
        onChanged();
      }
      if (!other.getScript().isEmpty()) {
        script_ = other.script_;
        onChanged();
      }
      if (!other.getOutputPathKey().isEmpty()) {
        outputPathKey_ = other.outputPathKey_;
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
      io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage =
            (io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object appId_ = "";
    /**
     * <code>string app_id = 1[json_name = "appId"];</code>
     * @return The appId.
     */
    public java.lang.String getAppId() {
      java.lang.Object ref = appId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        appId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string app_id = 1[json_name = "appId"];</code>
     * @return The bytes for appId.
     */
    public com.google.protobuf.ByteString getAppIdBytes() {
      java.lang.Object ref = appId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        appId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string app_id = 1[json_name = "appId"];</code>
     * @param value The appId to set.
     * @return This builder for chaining.
     */
    public Builder setAppId(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      appId_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string app_id = 1[json_name = "appId"];</code>
     * @return This builder for chaining.
     */
    public Builder clearAppId() {
      appId_ = getDefaultInstance().getAppId();
      onChanged();
      return this;
    }
    /**
     * <code>string app_id = 1[json_name = "appId"];</code>
     * @param value The bytes for appId to set.
     * @return This builder for chaining.
     */
    public Builder setAppIdBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      appId_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object accountId_ = "";
    /**
     * <code>string account_id = 2[json_name = "accountId"];</code>
     * @return The accountId.
     */
    public java.lang.String getAccountId() {
      java.lang.Object ref = accountId_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        accountId_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string account_id = 2[json_name = "accountId"];</code>
     * @return The bytes for accountId.
     */
    public com.google.protobuf.ByteString getAccountIdBytes() {
      java.lang.Object ref = accountId_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        accountId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string account_id = 2[json_name = "accountId"];</code>
     * @param value The accountId to set.
     * @return This builder for chaining.
     */
    public Builder setAccountId(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      accountId_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string account_id = 2[json_name = "accountId"];</code>
     * @return This builder for chaining.
     */
    public Builder clearAccountId() {
      accountId_ = getDefaultInstance().getAccountId();
      onChanged();
      return this;
    }
    /**
     * <code>string account_id = 2[json_name = "accountId"];</code>
     * @param value The bytes for accountId to set.
     * @return This builder for chaining.
     */
    public Builder setAccountIdBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      accountId_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object script_ = "";
    /**
     * <code>string script = 3[json_name = "script"];</code>
     * @return The script.
     */
    public java.lang.String getScript() {
      java.lang.Object ref = script_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        script_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string script = 3[json_name = "script"];</code>
     * @return The bytes for script.
     */
    public com.google.protobuf.ByteString getScriptBytes() {
      java.lang.Object ref = script_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        script_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string script = 3[json_name = "script"];</code>
     * @param value The script to set.
     * @return This builder for chaining.
     */
    public Builder setScript(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      script_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string script = 3[json_name = "script"];</code>
     * @return This builder for chaining.
     */
    public Builder clearScript() {
      script_ = getDefaultInstance().getScript();
      onChanged();
      return this;
    }
    /**
     * <code>string script = 3[json_name = "script"];</code>
     * @param value The bytes for script to set.
     * @return This builder for chaining.
     */
    public Builder setScriptBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      script_ = value;
      onChanged();
      return this;
    }

    private java.lang.Object outputPathKey_ = "";
    /**
     * <code>string output_path_key = 4[json_name = "outputPathKey"];</code>
     * @return The outputPathKey.
     */
    public java.lang.String getOutputPathKey() {
      java.lang.Object ref = outputPathKey_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        outputPathKey_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string output_path_key = 4[json_name = "outputPathKey"];</code>
     * @return The bytes for outputPathKey.
     */
    public com.google.protobuf.ByteString getOutputPathKeyBytes() {
      java.lang.Object ref = outputPathKey_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        outputPathKey_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string output_path_key = 4[json_name = "outputPathKey"];</code>
     * @param value The outputPathKey to set.
     * @return This builder for chaining.
     */
    public Builder setOutputPathKey(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      outputPathKey_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string output_path_key = 4[json_name = "outputPathKey"];</code>
     * @return This builder for chaining.
     */
    public Builder clearOutputPathKey() {
      outputPathKey_ = getDefaultInstance().getOutputPathKey();
      onChanged();
      return this;
    }
    /**
     * <code>string output_path_key = 4[json_name = "outputPathKey"];</code>
     * @param value The bytes for outputPathKey to set.
     * @return This builder for chaining.
     */
    public Builder setOutputPathKeyBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      outputPathKey_ = value;
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

    // @@protoc_insertion_point(builder_scope:io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams)
  }

  // @@protoc_insertion_point(class_scope:io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams)
  private static final io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams();
  }

  public static io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<CustomDeploymentInstanceSyncTaskParams> PARSER =
      new com.google.protobuf.AbstractParser<CustomDeploymentInstanceSyncTaskParams>() {
        @java.lang.Override
        public CustomDeploymentInstanceSyncTaskParams parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new CustomDeploymentInstanceSyncTaskParams(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<CustomDeploymentInstanceSyncTaskParams> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<CustomDeploymentInstanceSyncTaskParams> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.perpetualtask.instancesync.CustomDeploymentInstanceSyncTaskParams getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
