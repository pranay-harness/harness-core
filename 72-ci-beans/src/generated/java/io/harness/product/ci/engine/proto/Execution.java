// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/engine/proto/execution.proto

package io.harness.product.ci.engine.proto;

/**
 * Protobuf type {@code io.harness.product.ci.engine.proto.Execution}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:Execution.java.pb.meta")
public final class Execution extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.product.ci.engine.proto.Execution)
    ExecutionOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use Execution.newBuilder() to construct.
  private Execution(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Execution() {
    steps_ = java.util.Collections.emptyList();
  }

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new Execution();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private Execution(
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
            if (!((mutable_bitField0_ & 0x00000001) != 0)) {
              steps_ = new java.util.ArrayList<io.harness.product.ci.engine.proto.Step>();
              mutable_bitField0_ |= 0x00000001;
            }
            steps_.add(input.readMessage(io.harness.product.ci.engine.proto.Step.parser(), extensionRegistry));
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
      if (((mutable_bitField0_ & 0x00000001) != 0)) {
        steps_ = java.util.Collections.unmodifiableList(steps_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return io.harness.product.ci.engine.proto.ExecutionOuterClass
        .internal_static_io_harness_product_ci_engine_proto_Execution_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.product.ci.engine.proto.ExecutionOuterClass
        .internal_static_io_harness_product_ci_engine_proto_Execution_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.product.ci.engine.proto.Execution.class,
            io.harness.product.ci.engine.proto.Execution.Builder.class);
  }

  public static final int STEPS_FIELD_NUMBER = 1;
  private java.util.List<io.harness.product.ci.engine.proto.Step> steps_;
  /**
   * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
   */
  public java.util.List<io.harness.product.ci.engine.proto.Step> getStepsList() {
    return steps_;
  }
  /**
   * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
   */
  public java.util.List<? extends io.harness.product.ci.engine.proto.StepOrBuilder> getStepsOrBuilderList() {
    return steps_;
  }
  /**
   * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
   */
  public int getStepsCount() {
    return steps_.size();
  }
  /**
   * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
   */
  public io.harness.product.ci.engine.proto.Step getSteps(int index) {
    return steps_.get(index);
  }
  /**
   * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
   */
  public io.harness.product.ci.engine.proto.StepOrBuilder getStepsOrBuilder(int index) {
    return steps_.get(index);
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
    for (int i = 0; i < steps_.size(); i++) {
      output.writeMessage(1, steps_.get(i));
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    for (int i = 0; i < steps_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, steps_.get(i));
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
    if (!(obj instanceof io.harness.product.ci.engine.proto.Execution)) {
      return super.equals(obj);
    }
    io.harness.product.ci.engine.proto.Execution other = (io.harness.product.ci.engine.proto.Execution) obj;

    if (!getStepsList().equals(other.getStepsList()))
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
    if (getStepsCount() > 0) {
      hash = (37 * hash) + STEPS_FIELD_NUMBER;
      hash = (53 * hash) + getStepsList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.product.ci.engine.proto.Execution parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.product.ci.engine.proto.Execution parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.engine.proto.Execution parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.engine.proto.Execution parseFrom(com.google.protobuf.CodedInputStream input,
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
  public static Builder newBuilder(io.harness.product.ci.engine.proto.Execution prototype) {
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
   * Protobuf type {@code io.harness.product.ci.engine.proto.Execution}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.product.ci.engine.proto.Execution)
      io.harness.product.ci.engine.proto.ExecutionOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.product.ci.engine.proto.ExecutionOuterClass
          .internal_static_io_harness_product_ci_engine_proto_Execution_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.product.ci.engine.proto.ExecutionOuterClass
          .internal_static_io_harness_product_ci_engine_proto_Execution_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.product.ci.engine.proto.Execution.class,
              io.harness.product.ci.engine.proto.Execution.Builder.class);
    }

    // Construct using io.harness.product.ci.engine.proto.Execution.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        getStepsFieldBuilder();
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (stepsBuilder_ == null) {
        steps_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
      } else {
        stepsBuilder_.clear();
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.product.ci.engine.proto.ExecutionOuterClass
          .internal_static_io_harness_product_ci_engine_proto_Execution_descriptor;
    }

    @java.
    lang.Override
    public io.harness.product.ci.engine.proto.Execution getDefaultInstanceForType() {
      return io.harness.product.ci.engine.proto.Execution.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.product.ci.engine.proto.Execution build() {
      io.harness.product.ci.engine.proto.Execution result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.product.ci.engine.proto.Execution buildPartial() {
      io.harness.product.ci.engine.proto.Execution result = new io.harness.product.ci.engine.proto.Execution(this);
      int from_bitField0_ = bitField0_;
      if (stepsBuilder_ == null) {
        if (((bitField0_ & 0x00000001) != 0)) {
          steps_ = java.util.Collections.unmodifiableList(steps_);
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.steps_ = steps_;
      } else {
        result.steps_ = stepsBuilder_.build();
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
      if (other instanceof io.harness.product.ci.engine.proto.Execution) {
        return mergeFrom((io.harness.product.ci.engine.proto.Execution) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.product.ci.engine.proto.Execution other) {
      if (other == io.harness.product.ci.engine.proto.Execution.getDefaultInstance())
        return this;
      if (stepsBuilder_ == null) {
        if (!other.steps_.isEmpty()) {
          if (steps_.isEmpty()) {
            steps_ = other.steps_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureStepsIsMutable();
            steps_.addAll(other.steps_);
          }
          onChanged();
        }
      } else {
        if (!other.steps_.isEmpty()) {
          if (stepsBuilder_.isEmpty()) {
            stepsBuilder_.dispose();
            stepsBuilder_ = null;
            steps_ = other.steps_;
            bitField0_ = (bitField0_ & ~0x00000001);
            stepsBuilder_ =
                com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders ? getStepsFieldBuilder() : null;
          } else {
            stepsBuilder_.addAllMessages(other.steps_);
          }
        }
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
      io.harness.product.ci.engine.proto.Execution parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.product.ci.engine.proto.Execution) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.util.List<io.harness.product.ci.engine.proto.Step> steps_ = java.util.Collections.emptyList();
    private void ensureStepsIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        steps_ = new java.util.ArrayList<io.harness.product.ci.engine.proto.Step>(steps_);
        bitField0_ |= 0x00000001;
      }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<io.harness.product.ci.engine.proto.Step,
        io.harness.product.ci.engine.proto.Step.Builder, io.harness.product.ci.engine.proto.StepOrBuilder>
        stepsBuilder_;

    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public java.util.List<io.harness.product.ci.engine.proto.Step> getStepsList() {
      if (stepsBuilder_ == null) {
        return java.util.Collections.unmodifiableList(steps_);
      } else {
        return stepsBuilder_.getMessageList();
      }
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public int getStepsCount() {
      if (stepsBuilder_ == null) {
        return steps_.size();
      } else {
        return stepsBuilder_.getCount();
      }
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public io.harness.product.ci.engine.proto.Step getSteps(int index) {
      if (stepsBuilder_ == null) {
        return steps_.get(index);
      } else {
        return stepsBuilder_.getMessage(index);
      }
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder setSteps(int index, io.harness.product.ci.engine.proto.Step value) {
      if (stepsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureStepsIsMutable();
        steps_.set(index, value);
        onChanged();
      } else {
        stepsBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder setSteps(int index, io.harness.product.ci.engine.proto.Step.Builder builderForValue) {
      if (stepsBuilder_ == null) {
        ensureStepsIsMutable();
        steps_.set(index, builderForValue.build());
        onChanged();
      } else {
        stepsBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder addSteps(io.harness.product.ci.engine.proto.Step value) {
      if (stepsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureStepsIsMutable();
        steps_.add(value);
        onChanged();
      } else {
        stepsBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder addSteps(int index, io.harness.product.ci.engine.proto.Step value) {
      if (stepsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureStepsIsMutable();
        steps_.add(index, value);
        onChanged();
      } else {
        stepsBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder addSteps(io.harness.product.ci.engine.proto.Step.Builder builderForValue) {
      if (stepsBuilder_ == null) {
        ensureStepsIsMutable();
        steps_.add(builderForValue.build());
        onChanged();
      } else {
        stepsBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder addSteps(int index, io.harness.product.ci.engine.proto.Step.Builder builderForValue) {
      if (stepsBuilder_ == null) {
        ensureStepsIsMutable();
        steps_.add(index, builderForValue.build());
        onChanged();
      } else {
        stepsBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder addAllSteps(java.lang.Iterable<? extends io.harness.product.ci.engine.proto.Step> values) {
      if (stepsBuilder_ == null) {
        ensureStepsIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, steps_);
        onChanged();
      } else {
        stepsBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder clearSteps() {
      if (stepsBuilder_ == null) {
        steps_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
      } else {
        stepsBuilder_.clear();
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public Builder removeSteps(int index) {
      if (stepsBuilder_ == null) {
        ensureStepsIsMutable();
        steps_.remove(index);
        onChanged();
      } else {
        stepsBuilder_.remove(index);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public io.harness.product.ci.engine.proto.Step.Builder getStepsBuilder(int index) {
      return getStepsFieldBuilder().getBuilder(index);
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public io.harness.product.ci.engine.proto.StepOrBuilder getStepsOrBuilder(int index) {
      if (stepsBuilder_ == null) {
        return steps_.get(index);
      } else {
        return stepsBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public java.util.List<? extends io.harness.product.ci.engine.proto.StepOrBuilder> getStepsOrBuilderList() {
      if (stepsBuilder_ != null) {
        return stepsBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(steps_);
      }
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public io.harness.product.ci.engine.proto.Step.Builder addStepsBuilder() {
      return getStepsFieldBuilder().addBuilder(io.harness.product.ci.engine.proto.Step.getDefaultInstance());
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public io.harness.product.ci.engine.proto.Step.Builder addStepsBuilder(int index) {
      return getStepsFieldBuilder().addBuilder(index, io.harness.product.ci.engine.proto.Step.getDefaultInstance());
    }
    /**
     * <code>repeated .io.harness.product.ci.engine.proto.Step steps = 1[json_name = "steps"];</code>
     */
    public java.util.List<io.harness.product.ci.engine.proto.Step.Builder> getStepsBuilderList() {
      return getStepsFieldBuilder().getBuilderList();
    }
    private com.google.protobuf.RepeatedFieldBuilderV3<io.harness.product.ci.engine.proto.Step,
        io.harness.product.ci.engine.proto.Step.Builder, io.harness.product.ci.engine.proto.StepOrBuilder>
    getStepsFieldBuilder() {
      if (stepsBuilder_ == null) {
        stepsBuilder_ = new com.google.protobuf.RepeatedFieldBuilderV3<io.harness.product.ci.engine.proto.Step,
            io.harness.product.ci.engine.proto.Step.Builder, io.harness.product.ci.engine.proto.StepOrBuilder>(
            steps_, ((bitField0_ & 0x00000001) != 0), getParentForChildren(), isClean());
        steps_ = null;
      }
      return stepsBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.product.ci.engine.proto.Execution)
  }

  // @@protoc_insertion_point(class_scope:io.harness.product.ci.engine.proto.Execution)
  private static final io.harness.product.ci.engine.proto.Execution DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.product.ci.engine.proto.Execution();
  }

  public static io.harness.product.ci.engine.proto.Execution getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Execution> PARSER =
      new com.google.protobuf.AbstractParser<Execution>() {
        @java.lang.Override
        public Execution parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new Execution(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<Execution> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<Execution> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.product.ci.engine.proto.Execution getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
