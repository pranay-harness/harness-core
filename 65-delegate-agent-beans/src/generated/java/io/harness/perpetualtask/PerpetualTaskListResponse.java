// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task_service.proto

package io.harness.perpetualtask;

/**
 * Protobuf type {@code io.harness.perpetualtask.PerpetualTaskListResponse}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:PerpetualTaskListResponse.java.pb.meta")
public final class PerpetualTaskListResponse extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.perpetualtask.PerpetualTaskListResponse)
    PerpetualTaskListResponseOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use PerpetualTaskListResponse.newBuilder() to construct.
  private PerpetualTaskListResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private PerpetualTaskListResponse() {
    perpetualTaskAssignDetails_ = java.util.Collections.emptyList();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private PerpetualTaskListResponse(
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
              perpetualTaskAssignDetails_ =
                  new java.util.ArrayList<io.harness.perpetualtask.PerpetualTaskAssignDetails>();
              mutable_bitField0_ |= 0x00000001;
            }
            perpetualTaskAssignDetails_.add(
                input.readMessage(io.harness.perpetualtask.PerpetualTaskAssignDetails.parser(), extensionRegistry));
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
        perpetualTaskAssignDetails_ = java.util.Collections.unmodifiableList(perpetualTaskAssignDetails_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
        .internal_static_io_harness_perpetualtask_PerpetualTaskListResponse_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
        .internal_static_io_harness_perpetualtask_PerpetualTaskListResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.perpetualtask.PerpetualTaskListResponse.class,
            io.harness.perpetualtask.PerpetualTaskListResponse.Builder.class);
  }

  public static final int PERPETUAL_TASK_ASSIGN_DETAILS_FIELD_NUMBER = 1;
  private java.util.List<io.harness.perpetualtask.PerpetualTaskAssignDetails> perpetualTaskAssignDetails_;
  /**
   * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
   */
  public java.util.List<io.harness.perpetualtask.PerpetualTaskAssignDetails> getPerpetualTaskAssignDetailsList() {
    return perpetualTaskAssignDetails_;
  }
  /**
   * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
   */
  public java.util.List<? extends io.harness.perpetualtask.PerpetualTaskAssignDetailsOrBuilder>
  getPerpetualTaskAssignDetailsOrBuilderList() {
    return perpetualTaskAssignDetails_;
  }
  /**
   * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
   */
  public int getPerpetualTaskAssignDetailsCount() {
    return perpetualTaskAssignDetails_.size();
  }
  /**
   * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
   */
  public io.harness.perpetualtask.PerpetualTaskAssignDetails getPerpetualTaskAssignDetails(int index) {
    return perpetualTaskAssignDetails_.get(index);
  }
  /**
   * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
   */
  public io.harness.perpetualtask.PerpetualTaskAssignDetailsOrBuilder getPerpetualTaskAssignDetailsOrBuilder(
      int index) {
    return perpetualTaskAssignDetails_.get(index);
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
    for (int i = 0; i < perpetualTaskAssignDetails_.size(); i++) {
      output.writeMessage(1, perpetualTaskAssignDetails_.get(i));
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    for (int i = 0; i < perpetualTaskAssignDetails_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, perpetualTaskAssignDetails_.get(i));
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
    if (!(obj instanceof io.harness.perpetualtask.PerpetualTaskListResponse)) {
      return super.equals(obj);
    }
    io.harness.perpetualtask.PerpetualTaskListResponse other = (io.harness.perpetualtask.PerpetualTaskListResponse) obj;

    if (!getPerpetualTaskAssignDetailsList().equals(other.getPerpetualTaskAssignDetailsList()))
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
    if (getPerpetualTaskAssignDetailsCount() > 0) {
      hash = (37 * hash) + PERPETUAL_TASK_ASSIGN_DETAILS_FIELD_NUMBER;
      hash = (53 * hash) + getPerpetualTaskAssignDetailsList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.perpetualtask.PerpetualTaskListResponse parseFrom(com.google.protobuf.CodedInputStream input,
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
  public static Builder newBuilder(io.harness.perpetualtask.PerpetualTaskListResponse prototype) {
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
   * Protobuf type {@code io.harness.perpetualtask.PerpetualTaskListResponse}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.perpetualtask.PerpetualTaskListResponse)
      io.harness.perpetualtask.PerpetualTaskListResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
          .internal_static_io_harness_perpetualtask_PerpetualTaskListResponse_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
          .internal_static_io_harness_perpetualtask_PerpetualTaskListResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.perpetualtask.PerpetualTaskListResponse.class,
              io.harness.perpetualtask.PerpetualTaskListResponse.Builder.class);
    }

    // Construct using io.harness.perpetualtask.PerpetualTaskListResponse.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        getPerpetualTaskAssignDetailsFieldBuilder();
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        perpetualTaskAssignDetails_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
      } else {
        perpetualTaskAssignDetailsBuilder_.clear();
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.perpetualtask.PerpetualTaskServiceOuterClass
          .internal_static_io_harness_perpetualtask_PerpetualTaskListResponse_descriptor;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.PerpetualTaskListResponse getDefaultInstanceForType() {
      return io.harness.perpetualtask.PerpetualTaskListResponse.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.PerpetualTaskListResponse build() {
      io.harness.perpetualtask.PerpetualTaskListResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.perpetualtask.PerpetualTaskListResponse buildPartial() {
      io.harness.perpetualtask.PerpetualTaskListResponse result =
          new io.harness.perpetualtask.PerpetualTaskListResponse(this);
      int from_bitField0_ = bitField0_;
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        if (((bitField0_ & 0x00000001) != 0)) {
          perpetualTaskAssignDetails_ = java.util.Collections.unmodifiableList(perpetualTaskAssignDetails_);
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.perpetualTaskAssignDetails_ = perpetualTaskAssignDetails_;
      } else {
        result.perpetualTaskAssignDetails_ = perpetualTaskAssignDetailsBuilder_.build();
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
      if (other instanceof io.harness.perpetualtask.PerpetualTaskListResponse) {
        return mergeFrom((io.harness.perpetualtask.PerpetualTaskListResponse) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.perpetualtask.PerpetualTaskListResponse other) {
      if (other == io.harness.perpetualtask.PerpetualTaskListResponse.getDefaultInstance())
        return this;
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        if (!other.perpetualTaskAssignDetails_.isEmpty()) {
          if (perpetualTaskAssignDetails_.isEmpty()) {
            perpetualTaskAssignDetails_ = other.perpetualTaskAssignDetails_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensurePerpetualTaskAssignDetailsIsMutable();
            perpetualTaskAssignDetails_.addAll(other.perpetualTaskAssignDetails_);
          }
          onChanged();
        }
      } else {
        if (!other.perpetualTaskAssignDetails_.isEmpty()) {
          if (perpetualTaskAssignDetailsBuilder_.isEmpty()) {
            perpetualTaskAssignDetailsBuilder_.dispose();
            perpetualTaskAssignDetailsBuilder_ = null;
            perpetualTaskAssignDetails_ = other.perpetualTaskAssignDetails_;
            bitField0_ = (bitField0_ & ~0x00000001);
            perpetualTaskAssignDetailsBuilder_ = com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                ? getPerpetualTaskAssignDetailsFieldBuilder()
                : null;
          } else {
            perpetualTaskAssignDetailsBuilder_.addAllMessages(other.perpetualTaskAssignDetails_);
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
      io.harness.perpetualtask.PerpetualTaskListResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.perpetualtask.PerpetualTaskListResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.util.List<io.harness.perpetualtask.PerpetualTaskAssignDetails> perpetualTaskAssignDetails_ =
        java.util.Collections.emptyList();
    private void ensurePerpetualTaskAssignDetailsIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        perpetualTaskAssignDetails_ =
            new java.util.ArrayList<io.harness.perpetualtask.PerpetualTaskAssignDetails>(perpetualTaskAssignDetails_);
        bitField0_ |= 0x00000001;
      }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<io.harness.perpetualtask.PerpetualTaskAssignDetails,
        io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder,
        io.harness.perpetualtask.PerpetualTaskAssignDetailsOrBuilder> perpetualTaskAssignDetailsBuilder_;

    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public java.util.List<io.harness.perpetualtask.PerpetualTaskAssignDetails> getPerpetualTaskAssignDetailsList() {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        return java.util.Collections.unmodifiableList(perpetualTaskAssignDetails_);
      } else {
        return perpetualTaskAssignDetailsBuilder_.getMessageList();
      }
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public int getPerpetualTaskAssignDetailsCount() {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        return perpetualTaskAssignDetails_.size();
      } else {
        return perpetualTaskAssignDetailsBuilder_.getCount();
      }
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public io.harness.perpetualtask.PerpetualTaskAssignDetails getPerpetualTaskAssignDetails(int index) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        return perpetualTaskAssignDetails_.get(index);
      } else {
        return perpetualTaskAssignDetailsBuilder_.getMessage(index);
      }
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder setPerpetualTaskAssignDetails(int index, io.harness.perpetualtask.PerpetualTaskAssignDetails value) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePerpetualTaskAssignDetailsIsMutable();
        perpetualTaskAssignDetails_.set(index, value);
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder setPerpetualTaskAssignDetails(
        int index, io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder builderForValue) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        ensurePerpetualTaskAssignDetailsIsMutable();
        perpetualTaskAssignDetails_.set(index, builderForValue.build());
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder addPerpetualTaskAssignDetails(io.harness.perpetualtask.PerpetualTaskAssignDetails value) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePerpetualTaskAssignDetailsIsMutable();
        perpetualTaskAssignDetails_.add(value);
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder addPerpetualTaskAssignDetails(int index, io.harness.perpetualtask.PerpetualTaskAssignDetails value) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePerpetualTaskAssignDetailsIsMutable();
        perpetualTaskAssignDetails_.add(index, value);
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder addPerpetualTaskAssignDetails(
        io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder builderForValue) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        ensurePerpetualTaskAssignDetailsIsMutable();
        perpetualTaskAssignDetails_.add(builderForValue.build());
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder addPerpetualTaskAssignDetails(
        int index, io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder builderForValue) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        ensurePerpetualTaskAssignDetailsIsMutable();
        perpetualTaskAssignDetails_.add(index, builderForValue.build());
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder addAllPerpetualTaskAssignDetails(
        java.lang.Iterable<? extends io.harness.perpetualtask.PerpetualTaskAssignDetails> values) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        ensurePerpetualTaskAssignDetailsIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, perpetualTaskAssignDetails_);
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder clearPerpetualTaskAssignDetails() {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        perpetualTaskAssignDetails_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.clear();
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public Builder removePerpetualTaskAssignDetails(int index) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        ensurePerpetualTaskAssignDetailsIsMutable();
        perpetualTaskAssignDetails_.remove(index);
        onChanged();
      } else {
        perpetualTaskAssignDetailsBuilder_.remove(index);
      }
      return this;
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder getPerpetualTaskAssignDetailsBuilder(int index) {
      return getPerpetualTaskAssignDetailsFieldBuilder().getBuilder(index);
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public io.harness.perpetualtask.PerpetualTaskAssignDetailsOrBuilder getPerpetualTaskAssignDetailsOrBuilder(
        int index) {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        return perpetualTaskAssignDetails_.get(index);
      } else {
        return perpetualTaskAssignDetailsBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public java.util.List<? extends io.harness.perpetualtask.PerpetualTaskAssignDetailsOrBuilder>
    getPerpetualTaskAssignDetailsOrBuilderList() {
      if (perpetualTaskAssignDetailsBuilder_ != null) {
        return perpetualTaskAssignDetailsBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(perpetualTaskAssignDetails_);
      }
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder addPerpetualTaskAssignDetailsBuilder() {
      return getPerpetualTaskAssignDetailsFieldBuilder().addBuilder(
          io.harness.perpetualtask.PerpetualTaskAssignDetails.getDefaultInstance());
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder addPerpetualTaskAssignDetailsBuilder(int index) {
      return getPerpetualTaskAssignDetailsFieldBuilder().addBuilder(
          index, io.harness.perpetualtask.PerpetualTaskAssignDetails.getDefaultInstance());
    }
    /**
     * <code>repeated .io.harness.perpetualtask.PerpetualTaskAssignDetails perpetual_task_assign_details = 1;</code>
     */
    public java.util.List<io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder>
    getPerpetualTaskAssignDetailsBuilderList() {
      return getPerpetualTaskAssignDetailsFieldBuilder().getBuilderList();
    }
    private com.google.protobuf.RepeatedFieldBuilderV3<io.harness.perpetualtask.PerpetualTaskAssignDetails,
        io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder,
        io.harness.perpetualtask.PerpetualTaskAssignDetailsOrBuilder>
    getPerpetualTaskAssignDetailsFieldBuilder() {
      if (perpetualTaskAssignDetailsBuilder_ == null) {
        perpetualTaskAssignDetailsBuilder_ =
            new com.google.protobuf.RepeatedFieldBuilderV3<io.harness.perpetualtask.PerpetualTaskAssignDetails,
                io.harness.perpetualtask.PerpetualTaskAssignDetails.Builder,
                io.harness.perpetualtask.PerpetualTaskAssignDetailsOrBuilder>(
                perpetualTaskAssignDetails_, ((bitField0_ & 0x00000001) != 0), getParentForChildren(), isClean());
        perpetualTaskAssignDetails_ = null;
      }
      return perpetualTaskAssignDetailsBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.perpetualtask.PerpetualTaskListResponse)
  }

  // @@protoc_insertion_point(class_scope:io.harness.perpetualtask.PerpetualTaskListResponse)
  private static final io.harness.perpetualtask.PerpetualTaskListResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.perpetualtask.PerpetualTaskListResponse();
  }

  public static io.harness.perpetualtask.PerpetualTaskListResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PerpetualTaskListResponse> PARSER =
      new com.google.protobuf.AbstractParser<PerpetualTaskListResponse>() {
        @java.lang.Override
        public PerpetualTaskListResponse parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new PerpetualTaskListResponse(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<PerpetualTaskListResponse> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<PerpetualTaskListResponse> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.perpetualtask.PerpetualTaskListResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
