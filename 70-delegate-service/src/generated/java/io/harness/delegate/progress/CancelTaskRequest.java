// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: delegate_service.proto

package io.harness.delegate.progress;

/**
 * Protobuf type {@code io.harness.delegate.progress.CancelTaskRequest}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:CancelTaskRequest.java.pb.meta")
public final class CancelTaskRequest extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.delegate.progress.CancelTaskRequest)
    CancelTaskRequestOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use CancelTaskRequest.newBuilder() to construct.
  private CancelTaskRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private CancelTaskRequest() {}

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private CancelTaskRequest(
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
            io.harness.delegate.task.Id.Builder subBuilder = null;
            if (taskId_ != null) {
              subBuilder = taskId_.toBuilder();
            }
            taskId_ = input.readMessage(io.harness.delegate.task.Id.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(taskId_);
              taskId_ = subBuilder.buildPartial();
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
    return io.harness.delegate.progress.DelegateServiceOuterClass
        .internal_static_io_harness_delegate_progress_CancelTaskRequest_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.delegate.progress.DelegateServiceOuterClass
        .internal_static_io_harness_delegate_progress_CancelTaskRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.delegate.progress.CancelTaskRequest.class,
            io.harness.delegate.progress.CancelTaskRequest.Builder.class);
  }

  public static final int TASKID_FIELD_NUMBER = 1;
  private io.harness.delegate.task.Id taskId_;
  /**
   * <code>.io.harness.delegate.task.Id taskId = 1;</code>
   */
  public boolean hasTaskId() {
    return taskId_ != null;
  }
  /**
   * <code>.io.harness.delegate.task.Id taskId = 1;</code>
   */
  public io.harness.delegate.task.Id getTaskId() {
    return taskId_ == null ? io.harness.delegate.task.Id.getDefaultInstance() : taskId_;
  }
  /**
   * <code>.io.harness.delegate.task.Id taskId = 1;</code>
   */
  public io.harness.delegate.task.IdOrBuilder getTaskIdOrBuilder() {
    return getTaskId();
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
    if (taskId_ != null) {
      output.writeMessage(1, getTaskId());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (taskId_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getTaskId());
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
    if (!(obj instanceof io.harness.delegate.progress.CancelTaskRequest)) {
      return super.equals(obj);
    }
    io.harness.delegate.progress.CancelTaskRequest other = (io.harness.delegate.progress.CancelTaskRequest) obj;

    if (hasTaskId() != other.hasTaskId())
      return false;
    if (hasTaskId()) {
      if (!getTaskId().equals(other.getTaskId()))
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
    if (hasTaskId()) {
      hash = (37 * hash) + TASKID_FIELD_NUMBER;
      hash = (53 * hash) + getTaskId().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.progress.CancelTaskRequest parseFrom(com.google.protobuf.CodedInputStream input,
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
  public static Builder newBuilder(io.harness.delegate.progress.CancelTaskRequest prototype) {
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
   * Protobuf type {@code io.harness.delegate.progress.CancelTaskRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.delegate.progress.CancelTaskRequest)
      io.harness.delegate.progress.CancelTaskRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.delegate.progress.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_progress_CancelTaskRequest_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.delegate.progress.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_progress_CancelTaskRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.delegate.progress.CancelTaskRequest.class,
              io.harness.delegate.progress.CancelTaskRequest.Builder.class);
    }

    // Construct using io.harness.delegate.progress.CancelTaskRequest.newBuilder()
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
      if (taskIdBuilder_ == null) {
        taskId_ = null;
      } else {
        taskId_ = null;
        taskIdBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.delegate.progress.DelegateServiceOuterClass
          .internal_static_io_harness_delegate_progress_CancelTaskRequest_descriptor;
    }

    @java.
    lang.Override
    public io.harness.delegate.progress.CancelTaskRequest getDefaultInstanceForType() {
      return io.harness.delegate.progress.CancelTaskRequest.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.delegate.progress.CancelTaskRequest build() {
      io.harness.delegate.progress.CancelTaskRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.delegate.progress.CancelTaskRequest buildPartial() {
      io.harness.delegate.progress.CancelTaskRequest result = new io.harness.delegate.progress.CancelTaskRequest(this);
      if (taskIdBuilder_ == null) {
        result.taskId_ = taskId_;
      } else {
        result.taskId_ = taskIdBuilder_.build();
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
      if (other instanceof io.harness.delegate.progress.CancelTaskRequest) {
        return mergeFrom((io.harness.delegate.progress.CancelTaskRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.delegate.progress.CancelTaskRequest other) {
      if (other == io.harness.delegate.progress.CancelTaskRequest.getDefaultInstance())
        return this;
      if (other.hasTaskId()) {
        mergeTaskId(other.getTaskId());
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
      io.harness.delegate.progress.CancelTaskRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.delegate.progress.CancelTaskRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private io.harness.delegate.task.Id taskId_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.task.Id, io.harness.delegate.task.Id.Builder,
        io.harness.delegate.task.IdOrBuilder> taskIdBuilder_;
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    public boolean hasTaskId() {
      return taskIdBuilder_ != null || taskId_ != null;
    }
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    public io.harness.delegate.task.Id getTaskId() {
      if (taskIdBuilder_ == null) {
        return taskId_ == null ? io.harness.delegate.task.Id.getDefaultInstance() : taskId_;
      } else {
        return taskIdBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    public Builder setTaskId(io.harness.delegate.task.Id value) {
      if (taskIdBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        taskId_ = value;
        onChanged();
      } else {
        taskIdBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    public Builder setTaskId(io.harness.delegate.task.Id.Builder builderForValue) {
      if (taskIdBuilder_ == null) {
        taskId_ = builderForValue.build();
        onChanged();
      } else {
        taskIdBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    public Builder mergeTaskId(io.harness.delegate.task.Id value) {
      if (taskIdBuilder_ == null) {
        if (taskId_ != null) {
          taskId_ = io.harness.delegate.task.Id.newBuilder(taskId_).mergeFrom(value).buildPartial();
        } else {
          taskId_ = value;
        }
        onChanged();
      } else {
        taskIdBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    public Builder clearTaskId() {
      if (taskIdBuilder_ == null) {
        taskId_ = null;
        onChanged();
      } else {
        taskId_ = null;
        taskIdBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    public io.harness.delegate.task.Id.Builder getTaskIdBuilder() {
      onChanged();
      return getTaskIdFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    public io.harness.delegate.task.IdOrBuilder getTaskIdOrBuilder() {
      if (taskIdBuilder_ != null) {
        return taskIdBuilder_.getMessageOrBuilder();
      } else {
        return taskId_ == null ? io.harness.delegate.task.Id.getDefaultInstance() : taskId_;
      }
    }
    /**
     * <code>.io.harness.delegate.task.Id taskId = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.task.Id, io.harness.delegate.task.Id.Builder,
        io.harness.delegate.task.IdOrBuilder>
    getTaskIdFieldBuilder() {
      if (taskIdBuilder_ == null) {
        taskIdBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.task.Id,
            io.harness.delegate.task.Id.Builder, io.harness.delegate.task.IdOrBuilder>(
            getTaskId(), getParentForChildren(), isClean());
        taskId_ = null;
      }
      return taskIdBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.delegate.progress.CancelTaskRequest)
  }

  // @@protoc_insertion_point(class_scope:io.harness.delegate.progress.CancelTaskRequest)
  private static final io.harness.delegate.progress.CancelTaskRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.delegate.progress.CancelTaskRequest();
  }

  public static io.harness.delegate.progress.CancelTaskRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<CancelTaskRequest> PARSER =
      new com.google.protobuf.AbstractParser<CancelTaskRequest>() {
        @java.lang.Override
        public CancelTaskRequest parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new CancelTaskRequest(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<CancelTaskRequest> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<CancelTaskRequest> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.delegate.progress.CancelTaskRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
