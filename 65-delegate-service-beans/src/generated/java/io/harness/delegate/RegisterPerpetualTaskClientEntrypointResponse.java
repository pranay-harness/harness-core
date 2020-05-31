// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/delegate_service.proto

package io.harness.delegate;

/**
 * Protobuf type {@code io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse}
 */
@javax.annotation.
Generated(value = "protoc", comments = "annotations:RegisterPerpetualTaskClientEntrypointResponse.java.pb.meta")
public final class RegisterPerpetualTaskClientEntrypointResponse extends com.google.protobuf
                                                                             .GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse)
    RegisterPerpetualTaskClientEntrypointResponseOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use RegisterPerpetualTaskClientEntrypointResponse.newBuilder() to construct.
  private RegisterPerpetualTaskClientEntrypointResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private RegisterPerpetualTaskClientEntrypointResponse() {}

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private RegisterPerpetualTaskClientEntrypointResponse(
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
        .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.delegate.DelegateServiceOuterClass
        .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.class,
            io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.Builder.class);
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
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse)) {
      return super.equals(obj);
    }
    io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse other =
        (io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse) obj;

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
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(
      com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseDelimitedFrom(
      java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseDelimitedFrom(
      java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(
      com.google.protobuf.CodedInputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parseFrom(
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
  public static Builder newBuilder(io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse prototype) {
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
   * Protobuf type {@code io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse)
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.class,
              io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.Builder.class);
    }

    // Construct using io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.newBuilder()
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
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.delegate.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_descriptor;
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse getDefaultInstanceForType() {
      return io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse build() {
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse buildPartial() {
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse result =
          new io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse(this);
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
      if (other instanceof io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse) {
        return mergeFrom((io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse other) {
      if (other == io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.getDefaultInstance())
        return this;
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
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
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

    // @@protoc_insertion_point(builder_scope:io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse)
  }

  // @@protoc_insertion_point(class_scope:io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse)
  private static final io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse();
  }

  public static io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<RegisterPerpetualTaskClientEntrypointResponse> PARSER =
      new com.google.protobuf.AbstractParser<RegisterPerpetualTaskClientEntrypointResponse>() {
        @java.lang.Override
        public RegisterPerpetualTaskClientEntrypointResponse parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new RegisterPerpetualTaskClientEntrypointResponse(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<RegisterPerpetualTaskClientEntrypointResponse> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<RegisterPerpetualTaskClientEntrypointResponse> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
