// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/event/payloads/ec2_messages.proto

package io.harness.event.payloads;

/**
 * Protobuf type {@code io.harness.event.payloads.Ec2Lifecycle}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:Ec2Lifecycle.java.pb.meta")
public final class Ec2Lifecycle extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.event.payloads.Ec2Lifecycle)
    Ec2LifecycleOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use Ec2Lifecycle.newBuilder() to construct.
  private Ec2Lifecycle(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Ec2Lifecycle() {}

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private Ec2Lifecycle(
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
            io.harness.event.payloads.Lifecycle.Builder subBuilder = null;
            if (lifecycle_ != null) {
              subBuilder = lifecycle_.toBuilder();
            }
            lifecycle_ = input.readMessage(io.harness.event.payloads.Lifecycle.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(lifecycle_);
              lifecycle_ = subBuilder.buildPartial();
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
    return io.harness.event.payloads.Ec2Messages.internal_static_io_harness_event_payloads_Ec2Lifecycle_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.event.payloads.Ec2Messages
        .internal_static_io_harness_event_payloads_Ec2Lifecycle_fieldAccessorTable.ensureFieldAccessorsInitialized(
            io.harness.event.payloads.Ec2Lifecycle.class, io.harness.event.payloads.Ec2Lifecycle.Builder.class);
  }

  public static final int LIFECYCLE_FIELD_NUMBER = 1;
  private io.harness.event.payloads.Lifecycle lifecycle_;
  /**
   * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
   */
  public boolean hasLifecycle() {
    return lifecycle_ != null;
  }
  /**
   * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
   */
  public io.harness.event.payloads.Lifecycle getLifecycle() {
    return lifecycle_ == null ? io.harness.event.payloads.Lifecycle.getDefaultInstance() : lifecycle_;
  }
  /**
   * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
   */
  public io.harness.event.payloads.LifecycleOrBuilder getLifecycleOrBuilder() {
    return getLifecycle();
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
    if (lifecycle_ != null) {
      output.writeMessage(1, getLifecycle());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (lifecycle_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getLifecycle());
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
    if (!(obj instanceof io.harness.event.payloads.Ec2Lifecycle)) {
      return super.equals(obj);
    }
    io.harness.event.payloads.Ec2Lifecycle other = (io.harness.event.payloads.Ec2Lifecycle) obj;

    if (hasLifecycle() != other.hasLifecycle())
      return false;
    if (hasLifecycle()) {
      if (!getLifecycle().equals(other.getLifecycle()))
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
    if (hasLifecycle()) {
      hash = (37 * hash) + LIFECYCLE_FIELD_NUMBER;
      hash = (53 * hash) + getLifecycle().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.event.payloads.Ec2Lifecycle parseFrom(com.google.protobuf.CodedInputStream input,
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
  public static Builder newBuilder(io.harness.event.payloads.Ec2Lifecycle prototype) {
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
   * Protobuf type {@code io.harness.event.payloads.Ec2Lifecycle}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.event.payloads.Ec2Lifecycle)
      io.harness.event.payloads.Ec2LifecycleOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.event.payloads.Ec2Messages.internal_static_io_harness_event_payloads_Ec2Lifecycle_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.event.payloads.Ec2Messages
          .internal_static_io_harness_event_payloads_Ec2Lifecycle_fieldAccessorTable.ensureFieldAccessorsInitialized(
              io.harness.event.payloads.Ec2Lifecycle.class, io.harness.event.payloads.Ec2Lifecycle.Builder.class);
    }

    // Construct using io.harness.event.payloads.Ec2Lifecycle.newBuilder()
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
      if (lifecycleBuilder_ == null) {
        lifecycle_ = null;
      } else {
        lifecycle_ = null;
        lifecycleBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.event.payloads.Ec2Messages.internal_static_io_harness_event_payloads_Ec2Lifecycle_descriptor;
    }

    @java.
    lang.Override
    public io.harness.event.payloads.Ec2Lifecycle getDefaultInstanceForType() {
      return io.harness.event.payloads.Ec2Lifecycle.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.event.payloads.Ec2Lifecycle build() {
      io.harness.event.payloads.Ec2Lifecycle result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.event.payloads.Ec2Lifecycle buildPartial() {
      io.harness.event.payloads.Ec2Lifecycle result = new io.harness.event.payloads.Ec2Lifecycle(this);
      if (lifecycleBuilder_ == null) {
        result.lifecycle_ = lifecycle_;
      } else {
        result.lifecycle_ = lifecycleBuilder_.build();
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
      if (other instanceof io.harness.event.payloads.Ec2Lifecycle) {
        return mergeFrom((io.harness.event.payloads.Ec2Lifecycle) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.event.payloads.Ec2Lifecycle other) {
      if (other == io.harness.event.payloads.Ec2Lifecycle.getDefaultInstance())
        return this;
      if (other.hasLifecycle()) {
        mergeLifecycle(other.getLifecycle());
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
      io.harness.event.payloads.Ec2Lifecycle parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.event.payloads.Ec2Lifecycle) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private io.harness.event.payloads.Lifecycle lifecycle_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.Lifecycle,
        io.harness.event.payloads.Lifecycle.Builder, io.harness.event.payloads.LifecycleOrBuilder> lifecycleBuilder_;
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    public boolean hasLifecycle() {
      return lifecycleBuilder_ != null || lifecycle_ != null;
    }
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    public io.harness.event.payloads.Lifecycle getLifecycle() {
      if (lifecycleBuilder_ == null) {
        return lifecycle_ == null ? io.harness.event.payloads.Lifecycle.getDefaultInstance() : lifecycle_;
      } else {
        return lifecycleBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    public Builder setLifecycle(io.harness.event.payloads.Lifecycle value) {
      if (lifecycleBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        lifecycle_ = value;
        onChanged();
      } else {
        lifecycleBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    public Builder setLifecycle(io.harness.event.payloads.Lifecycle.Builder builderForValue) {
      if (lifecycleBuilder_ == null) {
        lifecycle_ = builderForValue.build();
        onChanged();
      } else {
        lifecycleBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    public Builder mergeLifecycle(io.harness.event.payloads.Lifecycle value) {
      if (lifecycleBuilder_ == null) {
        if (lifecycle_ != null) {
          lifecycle_ = io.harness.event.payloads.Lifecycle.newBuilder(lifecycle_).mergeFrom(value).buildPartial();
        } else {
          lifecycle_ = value;
        }
        onChanged();
      } else {
        lifecycleBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    public Builder clearLifecycle() {
      if (lifecycleBuilder_ == null) {
        lifecycle_ = null;
        onChanged();
      } else {
        lifecycle_ = null;
        lifecycleBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    public io.harness.event.payloads.Lifecycle.Builder getLifecycleBuilder() {
      onChanged();
      return getLifecycleFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    public io.harness.event.payloads.LifecycleOrBuilder getLifecycleOrBuilder() {
      if (lifecycleBuilder_ != null) {
        return lifecycleBuilder_.getMessageOrBuilder();
      } else {
        return lifecycle_ == null ? io.harness.event.payloads.Lifecycle.getDefaultInstance() : lifecycle_;
      }
    }
    /**
     * <code>.io.harness.event.payloads.Lifecycle lifecycle = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.Lifecycle,
        io.harness.event.payloads.Lifecycle.Builder, io.harness.event.payloads.LifecycleOrBuilder>
    getLifecycleFieldBuilder() {
      if (lifecycleBuilder_ == null) {
        lifecycleBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.Lifecycle,
            io.harness.event.payloads.Lifecycle.Builder, io.harness.event.payloads.LifecycleOrBuilder>(
            getLifecycle(), getParentForChildren(), isClean());
        lifecycle_ = null;
      }
      return lifecycleBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.event.payloads.Ec2Lifecycle)
  }

  // @@protoc_insertion_point(class_scope:io.harness.event.payloads.Ec2Lifecycle)
  private static final io.harness.event.payloads.Ec2Lifecycle DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.event.payloads.Ec2Lifecycle();
  }

  public static io.harness.event.payloads.Ec2Lifecycle getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Ec2Lifecycle> PARSER =
      new com.google.protobuf.AbstractParser<Ec2Lifecycle>() {
        @java.lang.Override
        public Ec2Lifecycle parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new Ec2Lifecycle(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<Ec2Lifecycle> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<Ec2Lifecycle> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.event.payloads.Ec2Lifecycle getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
