// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/ng_perpetualtask_service_client.proto

package io.harness.perpetualtask;

/**
 * Protobuf type {@code io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse}
 */
@javax.annotation.
Generated(value = "protoc", comments = "annotations:ObtainPerpetualTaskExecutionParamsResponse.java.pb.meta")
public final class ObtainPerpetualTaskExecutionParamsResponse extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse)
    ObtainPerpetualTaskExecutionParamsResponseOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use ObtainPerpetualTaskExecutionParamsResponse.newBuilder() to construct.
  private ObtainPerpetualTaskExecutionParamsResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private ObtainPerpetualTaskExecutionParamsResponse() {}

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new ObtainPerpetualTaskExecutionParamsResponse();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private ObtainPerpetualTaskExecutionParamsResponse(
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
            com.google.protobuf.Any.Builder subBuilder = null;
            if (customizedParams_ != null) {
              subBuilder = customizedParams_.toBuilder();
            }
            customizedParams_ = input.readMessage(com.google.protobuf.Any.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(customizedParams_);
              customizedParams_ = subBuilder.buildPartial();
            }

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
    return io.harness.perpetualtask.NgPerpetualtaskServiceClient
        .internal_static_io_harness_perpetualtask_ObtainPerpetualTaskExecutionParamsResponse_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.perpetualtask.NgPerpetualtaskServiceClient
        .internal_static_io_harness_perpetualtask_ObtainPerpetualTaskExecutionParamsResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse.class,
            io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse.Builder.class);
  }

  public static final int CUSTOMIZED_PARAMS_FIELD_NUMBER = 1;
  private com.google.protobuf.Any customizedParams_;
  /**
   * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
   * @return Whether the customizedParams field is set.
   */
  public boolean hasCustomizedParams() {
    return customizedParams_ != null;
  }
  /**
   * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
   * @return The customizedParams.
   */
  public com.google.protobuf.Any getCustomizedParams() {
    return customizedParams_ == null ? com.google.protobuf.Any.getDefaultInstance() : customizedParams_;
  }
  /**
   * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
   */
  public com.google.protobuf.AnyOrBuilder getCustomizedParamsOrBuilder() {
    return getCustomizedParams();
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
    if (customizedParams_ != null) {
      output.writeMessage(1, getCustomizedParams());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (customizedParams_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getCustomizedParams());
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
    if (!(obj instanceof io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse)) {
      return super.equals(obj);
    }
    io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse other =
        (io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse) obj;

    if (hasCustomizedParams() != other.hasCustomizedParams())
      return false;
    if (hasCustomizedParams()) {
      if (!getCustomizedParams().equals(other.getCustomizedParams()))
        return false;
    }
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
    if (hasCustomizedParams()) {
      hash = (37 * hash) + CUSTOMIZED_PARAMS_FIELD_NUMBER;
      hash = (53 * hash) + getCustomizedParams().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(
      com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parseFrom(
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
  public static Builder newBuilder(io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse prototype) {
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
   * Protobuf type {@code io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse)
      io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.perpetualtask.NgPerpetualtaskServiceClient
          .internal_static_io_harness_perpetualtask_ObtainPerpetualTaskExecutionParamsResponse_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.perpetualtask.NgPerpetualtaskServiceClient
          .internal_static_io_harness_perpetualtask_ObtainPerpetualTaskExecutionParamsResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse.class,
              io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse.Builder.class);
    }

    // Construct using io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse.newBuilder()
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
      if (customizedParamsBuilder_ == null) {
        customizedParams_ = null;
      } else {
        customizedParams_ = null;
        customizedParamsBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.perpetualtask.NgPerpetualtaskServiceClient
          .internal_static_io_harness_perpetualtask_ObtainPerpetualTaskExecutionParamsResponse_descriptor;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse getDefaultInstanceForType() {
      return io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse build() {
      io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse buildPartial() {
      io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse result =
          new io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse(this);
      if (customizedParamsBuilder_ == null) {
        result.customizedParams_ = customizedParams_;
      } else {
        result.customizedParams_ = customizedParamsBuilder_.build();
      }
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
      if (other instanceof io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse) {
        return mergeFrom((io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse other) {
      if (other == io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse.getDefaultInstance())
        return this;
      if (other.hasCustomizedParams()) {
        mergeCustomizedParams(other.getCustomizedParams());
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
      io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private com.google.protobuf.Any customizedParams_;
    private com.google.protobuf.SingleFieldBuilderV3<com.google.protobuf.Any, com.google.protobuf.Any.Builder,
        com.google.protobuf.AnyOrBuilder> customizedParamsBuilder_;
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     * @return Whether the customizedParams field is set.
     */
    public boolean hasCustomizedParams() {
      return customizedParamsBuilder_ != null || customizedParams_ != null;
    }
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     * @return The customizedParams.
     */
    public com.google.protobuf.Any getCustomizedParams() {
      if (customizedParamsBuilder_ == null) {
        return customizedParams_ == null ? com.google.protobuf.Any.getDefaultInstance() : customizedParams_;
      } else {
        return customizedParamsBuilder_.getMessage();
      }
    }
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     */
    public Builder setCustomizedParams(com.google.protobuf.Any value) {
      if (customizedParamsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        customizedParams_ = value;
        onChanged();
      } else {
        customizedParamsBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     */
    public Builder setCustomizedParams(com.google.protobuf.Any.Builder builderForValue) {
      if (customizedParamsBuilder_ == null) {
        customizedParams_ = builderForValue.build();
        onChanged();
      } else {
        customizedParamsBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     */
    public Builder mergeCustomizedParams(com.google.protobuf.Any value) {
      if (customizedParamsBuilder_ == null) {
        if (customizedParams_ != null) {
          customizedParams_ = com.google.protobuf.Any.newBuilder(customizedParams_).mergeFrom(value).buildPartial();
        } else {
          customizedParams_ = value;
        }
        onChanged();
      } else {
        customizedParamsBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     */
    public Builder clearCustomizedParams() {
      if (customizedParamsBuilder_ == null) {
        customizedParams_ = null;
        onChanged();
      } else {
        customizedParams_ = null;
        customizedParamsBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     */
    public com.google.protobuf.Any.Builder getCustomizedParamsBuilder() {
      onChanged();
      return getCustomizedParamsFieldBuilder().getBuilder();
    }
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     */
    public com.google.protobuf.AnyOrBuilder getCustomizedParamsOrBuilder() {
      if (customizedParamsBuilder_ != null) {
        return customizedParamsBuilder_.getMessageOrBuilder();
      } else {
        return customizedParams_ == null ? com.google.protobuf.Any.getDefaultInstance() : customizedParams_;
      }
    }
    /**
     * <code>.google.protobuf.Any customized_params = 1[json_name = "customizedParams"];</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<com.google.protobuf.Any, com.google.protobuf.Any.Builder,
        com.google.protobuf.AnyOrBuilder>
    getCustomizedParamsFieldBuilder() {
      if (customizedParamsBuilder_ == null) {
        customizedParamsBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<com.google.protobuf.Any, com.google.protobuf.Any.Builder,
                com.google.protobuf.AnyOrBuilder>(getCustomizedParams(), getParentForChildren(), isClean());
        customizedParams_ = null;
      }
      return customizedParamsBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse)
  }

  // @@protoc_insertion_point(class_scope:io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse)
  private static final io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse();
  }

  public static io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ObtainPerpetualTaskExecutionParamsResponse> PARSER =
      new com.google.protobuf.AbstractParser<ObtainPerpetualTaskExecutionParamsResponse>() {
        @java.lang.Override
        public ObtainPerpetualTaskExecutionParamsResponse parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new ObtainPerpetualTaskExecutionParamsResponse(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<ObtainPerpetualTaskExecutionParamsResponse> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<ObtainPerpetualTaskExecutionParamsResponse> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
