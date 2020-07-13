// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/delegate_service.proto

package io.harness.delegate;

/**
 * Protobuf type {@code io.harness.delegate.RegisterCallbackRequest}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:RegisterCallbackRequest.java.pb.meta")
public final class RegisterCallbackRequest extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.delegate.RegisterCallbackRequest)
    RegisterCallbackRequestOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use RegisterCallbackRequest.newBuilder() to construct.
  private RegisterCallbackRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private RegisterCallbackRequest() {}

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new RegisterCallbackRequest();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private RegisterCallbackRequest(
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
            io.harness.callback.DelegateCallback.Builder subBuilder = null;
            if (callback_ != null) {
              subBuilder = callback_.toBuilder();
            }
            callback_ = input.readMessage(io.harness.callback.DelegateCallback.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(callback_);
              callback_ = subBuilder.buildPartial();
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
    return io.harness.delegate.DelegateServiceOuterClass
        .internal_static_io_harness_delegate_RegisterCallbackRequest_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.delegate.DelegateServiceOuterClass
        .internal_static_io_harness_delegate_RegisterCallbackRequest_fieldAccessorTable.ensureFieldAccessorsInitialized(
            io.harness.delegate.RegisterCallbackRequest.class,
            io.harness.delegate.RegisterCallbackRequest.Builder.class);
  }

  public static final int CALLBACK_FIELD_NUMBER = 1;
  private io.harness.callback.DelegateCallback callback_;
  /**
   * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
   * @return Whether the callback field is set.
   */
  public boolean hasCallback() {
    return callback_ != null;
  }
  /**
   * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
   * @return The callback.
   */
  public io.harness.callback.DelegateCallback getCallback() {
    return callback_ == null ? io.harness.callback.DelegateCallback.getDefaultInstance() : callback_;
  }
  /**
   * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
   */
  public io.harness.callback.DelegateCallbackOrBuilder getCallbackOrBuilder() {
    return getCallback();
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
    if (callback_ != null) {
      output.writeMessage(1, getCallback());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (callback_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getCallback());
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
    if (!(obj instanceof io.harness.delegate.RegisterCallbackRequest)) {
      return super.equals(obj);
    }
    io.harness.delegate.RegisterCallbackRequest other = (io.harness.delegate.RegisterCallbackRequest) obj;

    if (hasCallback() != other.hasCallback())
      return false;
    if (hasCallback()) {
      if (!getCallback().equals(other.getCallback()))
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
    if (hasCallback()) {
      hash = (37 * hash) + CALLBACK_FIELD_NUMBER;
      hash = (53 * hash) + getCallback().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.delegate.RegisterCallbackRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterCallbackRequest parseFrom(com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() {
    return newBuilder();
  }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(io.harness.delegate.RegisterCallbackRequest prototype) {
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
   * Protobuf type {@code io.harness.delegate.RegisterCallbackRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.delegate.RegisterCallbackRequest)
      io.harness.delegate.RegisterCallbackRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterCallbackRequest_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterCallbackRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.delegate.RegisterCallbackRequest.class,
              io.harness.delegate.RegisterCallbackRequest.Builder.class);
    }

    // Construct using io.harness.delegate.RegisterCallbackRequest.newBuilder()
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
      if (callbackBuilder_ == null) {
        callback_ = null;
      } else {
        callback_ = null;
        callbackBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterCallbackRequest_descriptor;
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterCallbackRequest getDefaultInstanceForType() {
      return io.harness.delegate.RegisterCallbackRequest.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterCallbackRequest build() {
      io.harness.delegate.RegisterCallbackRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterCallbackRequest buildPartial() {
      io.harness.delegate.RegisterCallbackRequest result = new io.harness.delegate.RegisterCallbackRequest(this);
      if (callbackBuilder_ == null) {
        result.callback_ = callback_;
      } else {
        result.callback_ = callbackBuilder_.build();
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
      if (other instanceof io.harness.delegate.RegisterCallbackRequest) {
        return mergeFrom((io.harness.delegate.RegisterCallbackRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.delegate.RegisterCallbackRequest other) {
      if (other == io.harness.delegate.RegisterCallbackRequest.getDefaultInstance())
        return this;
      if (other.hasCallback()) {
        mergeCallback(other.getCallback());
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
      io.harness.delegate.RegisterCallbackRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.delegate.RegisterCallbackRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private io.harness.callback.DelegateCallback callback_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.callback.DelegateCallback,
        io.harness.callback.DelegateCallback.Builder, io.harness.callback.DelegateCallbackOrBuilder> callbackBuilder_;
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     * @return Whether the callback field is set.
     */
    public boolean hasCallback() {
      return callbackBuilder_ != null || callback_ != null;
    }
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     * @return The callback.
     */
    public io.harness.callback.DelegateCallback getCallback() {
      if (callbackBuilder_ == null) {
        return callback_ == null ? io.harness.callback.DelegateCallback.getDefaultInstance() : callback_;
      } else {
        return callbackBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     */
    public Builder setCallback(io.harness.callback.DelegateCallback value) {
      if (callbackBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        callback_ = value;
        onChanged();
      } else {
        callbackBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     */
    public Builder setCallback(io.harness.callback.DelegateCallback.Builder builderForValue) {
      if (callbackBuilder_ == null) {
        callback_ = builderForValue.build();
        onChanged();
      } else {
        callbackBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     */
    public Builder mergeCallback(io.harness.callback.DelegateCallback value) {
      if (callbackBuilder_ == null) {
        if (callback_ != null) {
          callback_ = io.harness.callback.DelegateCallback.newBuilder(callback_).mergeFrom(value).buildPartial();
        } else {
          callback_ = value;
        }
        onChanged();
      } else {
        callbackBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     */
    public Builder clearCallback() {
      if (callbackBuilder_ == null) {
        callback_ = null;
        onChanged();
      } else {
        callback_ = null;
        callbackBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     */
    public io.harness.callback.DelegateCallback.Builder getCallbackBuilder() {
      onChanged();
      return getCallbackFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     */
    public io.harness.callback.DelegateCallbackOrBuilder getCallbackOrBuilder() {
      if (callbackBuilder_ != null) {
        return callbackBuilder_.getMessageOrBuilder();
      } else {
        return callback_ == null ? io.harness.callback.DelegateCallback.getDefaultInstance() : callback_;
      }
    }
    /**
     * <code>.io.harness.callback.DelegateCallback callback = 1[json_name = "callback"];</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.callback.DelegateCallback,
        io.harness.callback.DelegateCallback.Builder, io.harness.callback.DelegateCallbackOrBuilder>
    getCallbackFieldBuilder() {
      if (callbackBuilder_ == null) {
        callbackBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<io.harness.callback.DelegateCallback,
            io.harness.callback.DelegateCallback.Builder, io.harness.callback.DelegateCallbackOrBuilder>(
            getCallback(), getParentForChildren(), isClean());
        callback_ = null;
      }
      return callbackBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.delegate.RegisterCallbackRequest)
  }

  // @@protoc_insertion_point(class_scope:io.harness.delegate.RegisterCallbackRequest)
  private static final io.harness.delegate.RegisterCallbackRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.delegate.RegisterCallbackRequest();
  }

  public static io.harness.delegate.RegisterCallbackRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<RegisterCallbackRequest> PARSER =
      new com.google.protobuf.AbstractParser<RegisterCallbackRequest>() {
        @java.lang.Override
        public RegisterCallbackRequest parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new RegisterCallbackRequest(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<RegisterCallbackRequest> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<RegisterCallbackRequest> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.delegate.RegisterCallbackRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
