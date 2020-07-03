// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/addon/proto/addon.proto

package io.harness.product.ci.addon.proto;

/**
 * Protobuf type {@code io.harness.product.ci.addon.proto.TaskProgressResponse}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:TaskProgressResponse.java.pb.meta")
public final class TaskProgressResponse extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.product.ci.addon.proto.TaskProgressResponse)
    TaskProgressResponseOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use TaskProgressResponse.newBuilder() to construct.
  private TaskProgressResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private TaskProgressResponse() {
    currentStage_ = 0;
  }

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new TaskProgressResponse();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private TaskProgressResponse(
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
          case 8: {
            int rawValue = input.readEnum();

            currentStage_ = rawValue;
            break;
          }
          case 18: {
            io.harness.product.ci.addon.proto.Error.Builder subBuilder = null;
            if (error_ != null) {
              subBuilder = error_.toBuilder();
            }
            error_ = input.readMessage(io.harness.product.ci.addon.proto.Error.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(error_);
              error_ = subBuilder.buildPartial();
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
    return io.harness.product.ci.addon.proto.Addon
        .internal_static_io_harness_product_ci_addon_proto_TaskProgressResponse_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.product.ci.addon.proto.Addon
        .internal_static_io_harness_product_ci_addon_proto_TaskProgressResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.product.ci.addon.proto.TaskProgressResponse.class,
            io.harness.product.ci.addon.proto.TaskProgressResponse.Builder.class);
  }

  public static final int CURRENT_STAGE_FIELD_NUMBER = 1;
  private int currentStage_;
  /**
   * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
   * @return The enum numeric value on the wire for currentStage.
   */
  public int getCurrentStageValue() {
    return currentStage_;
  }
  /**
   * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
   * @return The currentStage.
   */
  public io.harness.product.ci.addon.proto.TaskStatus getCurrentStage() {
    @SuppressWarnings("deprecation")
    io.harness.product.ci.addon.proto.TaskStatus result =
        io.harness.product.ci.addon.proto.TaskStatus.valueOf(currentStage_);
    return result == null ? io.harness.product.ci.addon.proto.TaskStatus.UNRECOGNIZED : result;
  }

  public static final int ERROR_FIELD_NUMBER = 2;
  private io.harness.product.ci.addon.proto.Error error_;
  /**
   * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
   * @return Whether the error field is set.
   */
  public boolean hasError() {
    return error_ != null;
  }
  /**
   * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
   * @return The error.
   */
  public io.harness.product.ci.addon.proto.Error getError() {
    return error_ == null ? io.harness.product.ci.addon.proto.Error.getDefaultInstance() : error_;
  }
  /**
   * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
   */
  public io.harness.product.ci.addon.proto.ErrorOrBuilder getErrorOrBuilder() {
    return getError();
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
    if (currentStage_ != io.harness.product.ci.addon.proto.TaskStatus.PENDING.getNumber()) {
      output.writeEnum(1, currentStage_);
    }
    if (error_ != null) {
      output.writeMessage(2, getError());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (currentStage_ != io.harness.product.ci.addon.proto.TaskStatus.PENDING.getNumber()) {
      size += com.google.protobuf.CodedOutputStream.computeEnumSize(1, currentStage_);
    }
    if (error_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getError());
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
    if (!(obj instanceof io.harness.product.ci.addon.proto.TaskProgressResponse)) {
      return super.equals(obj);
    }
    io.harness.product.ci.addon.proto.TaskProgressResponse other =
        (io.harness.product.ci.addon.proto.TaskProgressResponse) obj;

    if (currentStage_ != other.currentStage_)
      return false;
    if (hasError() != other.hasError())
      return false;
    if (hasError()) {
      if (!getError().equals(other.getError()))
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
    hash = (37 * hash) + CURRENT_STAGE_FIELD_NUMBER;
    hash = (53 * hash) + currentStage_;
    if (hasError()) {
      hash = (37 * hash) + ERROR_FIELD_NUMBER;
      hash = (53 * hash) + getError().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.addon.proto.TaskProgressResponse parseFrom(
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
  public static Builder newBuilder(io.harness.product.ci.addon.proto.TaskProgressResponse prototype) {
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
   * Protobuf type {@code io.harness.product.ci.addon.proto.TaskProgressResponse}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.product.ci.addon.proto.TaskProgressResponse)
      io.harness.product.ci.addon.proto.TaskProgressResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.product.ci.addon.proto.Addon
          .internal_static_io_harness_product_ci_addon_proto_TaskProgressResponse_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.product.ci.addon.proto.Addon
          .internal_static_io_harness_product_ci_addon_proto_TaskProgressResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.product.ci.addon.proto.TaskProgressResponse.class,
              io.harness.product.ci.addon.proto.TaskProgressResponse.Builder.class);
    }

    // Construct using io.harness.product.ci.addon.proto.TaskProgressResponse.newBuilder()
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
      currentStage_ = 0;

      if (errorBuilder_ == null) {
        error_ = null;
      } else {
        error_ = null;
        errorBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.product.ci.addon.proto.Addon
          .internal_static_io_harness_product_ci_addon_proto_TaskProgressResponse_descriptor;
    }

    @java.
    lang.Override
    public io.harness.product.ci.addon.proto.TaskProgressResponse getDefaultInstanceForType() {
      return io.harness.product.ci.addon.proto.TaskProgressResponse.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.product.ci.addon.proto.TaskProgressResponse build() {
      io.harness.product.ci.addon.proto.TaskProgressResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.product.ci.addon.proto.TaskProgressResponse buildPartial() {
      io.harness.product.ci.addon.proto.TaskProgressResponse result =
          new io.harness.product.ci.addon.proto.TaskProgressResponse(this);
      result.currentStage_ = currentStage_;
      if (errorBuilder_ == null) {
        result.error_ = error_;
      } else {
        result.error_ = errorBuilder_.build();
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
      if (other instanceof io.harness.product.ci.addon.proto.TaskProgressResponse) {
        return mergeFrom((io.harness.product.ci.addon.proto.TaskProgressResponse) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.product.ci.addon.proto.TaskProgressResponse other) {
      if (other == io.harness.product.ci.addon.proto.TaskProgressResponse.getDefaultInstance())
        return this;
      if (other.currentStage_ != 0) {
        setCurrentStageValue(other.getCurrentStageValue());
      }
      if (other.hasError()) {
        mergeError(other.getError());
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
      io.harness.product.ci.addon.proto.TaskProgressResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.product.ci.addon.proto.TaskProgressResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int currentStage_ = 0;
    /**
     * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
     * @return The enum numeric value on the wire for currentStage.
     */
    public int getCurrentStageValue() {
      return currentStage_;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
     * @param value The enum numeric value on the wire for currentStage to set.
     * @return This builder for chaining.
     */
    public Builder setCurrentStageValue(int value) {
      currentStage_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
     * @return The currentStage.
     */
    public io.harness.product.ci.addon.proto.TaskStatus getCurrentStage() {
      @SuppressWarnings("deprecation")
      io.harness.product.ci.addon.proto.TaskStatus result =
          io.harness.product.ci.addon.proto.TaskStatus.valueOf(currentStage_);
      return result == null ? io.harness.product.ci.addon.proto.TaskStatus.UNRECOGNIZED : result;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
     * @param value The currentStage to set.
     * @return This builder for chaining.
     */
    public Builder setCurrentStage(io.harness.product.ci.addon.proto.TaskStatus value) {
      if (value == null) {
        throw new NullPointerException();
      }

      currentStage_ = value.getNumber();
      onChanged();
      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
     * @return This builder for chaining.
     */
    public Builder clearCurrentStage() {
      currentStage_ = 0;
      onChanged();
      return this;
    }

    private io.harness.product.ci.addon.proto.Error error_;
    private com.google.protobuf
        .SingleFieldBuilderV3<io.harness.product.ci.addon.proto.Error, io.harness.product.ci.addon.proto.Error.Builder,
            io.harness.product.ci.addon.proto.ErrorOrBuilder> errorBuilder_;
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     * @return Whether the error field is set.
     */
    public boolean hasError() {
      return errorBuilder_ != null || error_ != null;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     * @return The error.
     */
    public io.harness.product.ci.addon.proto.Error getError() {
      if (errorBuilder_ == null) {
        return error_ == null ? io.harness.product.ci.addon.proto.Error.getDefaultInstance() : error_;
      } else {
        return errorBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     */
    public Builder setError(io.harness.product.ci.addon.proto.Error value) {
      if (errorBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        error_ = value;
        onChanged();
      } else {
        errorBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     */
    public Builder setError(io.harness.product.ci.addon.proto.Error.Builder builderForValue) {
      if (errorBuilder_ == null) {
        error_ = builderForValue.build();
        onChanged();
      } else {
        errorBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     */
    public Builder mergeError(io.harness.product.ci.addon.proto.Error value) {
      if (errorBuilder_ == null) {
        if (error_ != null) {
          error_ = io.harness.product.ci.addon.proto.Error.newBuilder(error_).mergeFrom(value).buildPartial();
        } else {
          error_ = value;
        }
        onChanged();
      } else {
        errorBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     */
    public Builder clearError() {
      if (errorBuilder_ == null) {
        error_ = null;
        onChanged();
      } else {
        error_ = null;
        errorBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     */
    public io.harness.product.ci.addon.proto.Error.Builder getErrorBuilder() {
      onChanged();
      return getErrorFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     */
    public io.harness.product.ci.addon.proto.ErrorOrBuilder getErrorOrBuilder() {
      if (errorBuilder_ != null) {
        return errorBuilder_.getMessageOrBuilder();
      } else {
        return error_ == null ? io.harness.product.ci.addon.proto.Error.getDefaultInstance() : error_;
      }
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.product.ci.addon.proto.Error,
        io.harness.product.ci.addon.proto.Error.Builder, io.harness.product.ci.addon.proto.ErrorOrBuilder>
    getErrorFieldBuilder() {
      if (errorBuilder_ == null) {
        errorBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<io.harness.product.ci.addon.proto.Error,
            io.harness.product.ci.addon.proto.Error.Builder, io.harness.product.ci.addon.proto.ErrorOrBuilder>(
            getError(), getParentForChildren(), isClean());
        error_ = null;
      }
      return errorBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.product.ci.addon.proto.TaskProgressResponse)
  }

  // @@protoc_insertion_point(class_scope:io.harness.product.ci.addon.proto.TaskProgressResponse)
  private static final io.harness.product.ci.addon.proto.TaskProgressResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.product.ci.addon.proto.TaskProgressResponse();
  }

  public static io.harness.product.ci.addon.proto.TaskProgressResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<TaskProgressResponse> PARSER =
      new com.google.protobuf.AbstractParser<TaskProgressResponse>() {
        @java.lang.Override
        public TaskProgressResponse parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new TaskProgressResponse(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<TaskProgressResponse> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<TaskProgressResponse> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.product.ci.addon.proto.TaskProgressResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
