// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/event/payloads/ecs_messages.proto

package io.harness.event.payloads;

/**
 * Protobuf type {@code io.harness.event.payloads.EcsContainerInstanceInfo}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:EcsContainerInstanceInfo.java.pb.meta")
public final class EcsContainerInstanceInfo extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.event.payloads.EcsContainerInstanceInfo)
    EcsContainerInstanceInfoOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use EcsContainerInstanceInfo.newBuilder() to construct.
  private EcsContainerInstanceInfo(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private EcsContainerInstanceInfo() {}

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new EcsContainerInstanceInfo();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private EcsContainerInstanceInfo(
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
            io.harness.event.payloads.EcsContainerInstanceDescription.Builder subBuilder = null;
            if (ecsContainerInstanceDescription_ != null) {
              subBuilder = ecsContainerInstanceDescription_.toBuilder();
            }
            ecsContainerInstanceDescription_ = input.readMessage(
                io.harness.event.payloads.EcsContainerInstanceDescription.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(ecsContainerInstanceDescription_);
              ecsContainerInstanceDescription_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
            io.harness.event.payloads.ReservedResource.Builder subBuilder = null;
            if (ecsContainerInstanceResource_ != null) {
              subBuilder = ecsContainerInstanceResource_.toBuilder();
            }
            ecsContainerInstanceResource_ =
                input.readMessage(io.harness.event.payloads.ReservedResource.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(ecsContainerInstanceResource_);
              ecsContainerInstanceResource_ = subBuilder.buildPartial();
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
    return io.harness.event.payloads.EcsMessages
        .internal_static_io_harness_event_payloads_EcsContainerInstanceInfo_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.event.payloads.EcsMessages
        .internal_static_io_harness_event_payloads_EcsContainerInstanceInfo_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.event.payloads.EcsContainerInstanceInfo.class,
            io.harness.event.payloads.EcsContainerInstanceInfo.Builder.class);
  }

  public static final int ECS_CONTAINER_INSTANCE_DESCRIPTION_FIELD_NUMBER = 1;
  private io.harness.event.payloads.EcsContainerInstanceDescription ecsContainerInstanceDescription_;
  /**
   * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name =
   * "ecsContainerInstanceDescription"];</code>
   * @return Whether the ecsContainerInstanceDescription field is set.
   */
  public boolean hasEcsContainerInstanceDescription() {
    return ecsContainerInstanceDescription_ != null;
  }
  /**
   * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name =
   * "ecsContainerInstanceDescription"];</code>
   * @return The ecsContainerInstanceDescription.
   */
  public io.harness.event.payloads.EcsContainerInstanceDescription getEcsContainerInstanceDescription() {
    return ecsContainerInstanceDescription_ == null
        ? io.harness.event.payloads.EcsContainerInstanceDescription.getDefaultInstance()
        : ecsContainerInstanceDescription_;
  }
  /**
   * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name =
   * "ecsContainerInstanceDescription"];</code>
   */
  public io.harness.event.payloads.EcsContainerInstanceDescriptionOrBuilder
  getEcsContainerInstanceDescriptionOrBuilder() {
    return getEcsContainerInstanceDescription();
  }

  public static final int ECS_CONTAINER_INSTANCE_RESOURCE_FIELD_NUMBER = 2;
  private io.harness.event.payloads.ReservedResource ecsContainerInstanceResource_;
  /**
   * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
   * "ecsContainerInstanceResource"];</code>
   * @return Whether the ecsContainerInstanceResource field is set.
   */
  public boolean hasEcsContainerInstanceResource() {
    return ecsContainerInstanceResource_ != null;
  }
  /**
   * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
   * "ecsContainerInstanceResource"];</code>
   * @return The ecsContainerInstanceResource.
   */
  public io.harness.event.payloads.ReservedResource getEcsContainerInstanceResource() {
    return ecsContainerInstanceResource_ == null ? io.harness.event.payloads.ReservedResource.getDefaultInstance()
                                                 : ecsContainerInstanceResource_;
  }
  /**
   * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
   * "ecsContainerInstanceResource"];</code>
   */
  public io.harness.event.payloads.ReservedResourceOrBuilder getEcsContainerInstanceResourceOrBuilder() {
    return getEcsContainerInstanceResource();
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
    if (ecsContainerInstanceDescription_ != null) {
      output.writeMessage(1, getEcsContainerInstanceDescription());
    }
    if (ecsContainerInstanceResource_ != null) {
      output.writeMessage(2, getEcsContainerInstanceResource());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (ecsContainerInstanceDescription_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getEcsContainerInstanceDescription());
    }
    if (ecsContainerInstanceResource_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getEcsContainerInstanceResource());
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
    if (!(obj instanceof io.harness.event.payloads.EcsContainerInstanceInfo)) {
      return super.equals(obj);
    }
    io.harness.event.payloads.EcsContainerInstanceInfo other = (io.harness.event.payloads.EcsContainerInstanceInfo) obj;

    if (hasEcsContainerInstanceDescription() != other.hasEcsContainerInstanceDescription())
      return false;
    if (hasEcsContainerInstanceDescription()) {
      if (!getEcsContainerInstanceDescription().equals(other.getEcsContainerInstanceDescription()))
        return false;
    }
    if (hasEcsContainerInstanceResource() != other.hasEcsContainerInstanceResource())
      return false;
    if (hasEcsContainerInstanceResource()) {
      if (!getEcsContainerInstanceResource().equals(other.getEcsContainerInstanceResource()))
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
    if (hasEcsContainerInstanceDescription()) {
      hash = (37 * hash) + ECS_CONTAINER_INSTANCE_DESCRIPTION_FIELD_NUMBER;
      hash = (53 * hash) + getEcsContainerInstanceDescription().hashCode();
    }
    if (hasEcsContainerInstanceResource()) {
      hash = (37 * hash) + ECS_CONTAINER_INSTANCE_RESOURCE_FIELD_NUMBER;
      hash = (53 * hash) + getEcsContainerInstanceResource().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.event.payloads.EcsContainerInstanceInfo parseFrom(com.google.protobuf.CodedInputStream input,
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
  public static Builder newBuilder(io.harness.event.payloads.EcsContainerInstanceInfo prototype) {
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
   * Protobuf type {@code io.harness.event.payloads.EcsContainerInstanceInfo}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.event.payloads.EcsContainerInstanceInfo)
      io.harness.event.payloads.EcsContainerInstanceInfoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.event.payloads.EcsMessages
          .internal_static_io_harness_event_payloads_EcsContainerInstanceInfo_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.event.payloads.EcsMessages
          .internal_static_io_harness_event_payloads_EcsContainerInstanceInfo_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.event.payloads.EcsContainerInstanceInfo.class,
              io.harness.event.payloads.EcsContainerInstanceInfo.Builder.class);
    }

    // Construct using io.harness.event.payloads.EcsContainerInstanceInfo.newBuilder()
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
      if (ecsContainerInstanceDescriptionBuilder_ == null) {
        ecsContainerInstanceDescription_ = null;
      } else {
        ecsContainerInstanceDescription_ = null;
        ecsContainerInstanceDescriptionBuilder_ = null;
      }
      if (ecsContainerInstanceResourceBuilder_ == null) {
        ecsContainerInstanceResource_ = null;
      } else {
        ecsContainerInstanceResource_ = null;
        ecsContainerInstanceResourceBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.event.payloads.EcsMessages
          .internal_static_io_harness_event_payloads_EcsContainerInstanceInfo_descriptor;
    }

    @java.
    lang.Override
    public io.harness.event.payloads.EcsContainerInstanceInfo getDefaultInstanceForType() {
      return io.harness.event.payloads.EcsContainerInstanceInfo.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.event.payloads.EcsContainerInstanceInfo build() {
      io.harness.event.payloads.EcsContainerInstanceInfo result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.event.payloads.EcsContainerInstanceInfo buildPartial() {
      io.harness.event.payloads.EcsContainerInstanceInfo result =
          new io.harness.event.payloads.EcsContainerInstanceInfo(this);
      if (ecsContainerInstanceDescriptionBuilder_ == null) {
        result.ecsContainerInstanceDescription_ = ecsContainerInstanceDescription_;
      } else {
        result.ecsContainerInstanceDescription_ = ecsContainerInstanceDescriptionBuilder_.build();
      }
      if (ecsContainerInstanceResourceBuilder_ == null) {
        result.ecsContainerInstanceResource_ = ecsContainerInstanceResource_;
      } else {
        result.ecsContainerInstanceResource_ = ecsContainerInstanceResourceBuilder_.build();
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
      if (other instanceof io.harness.event.payloads.EcsContainerInstanceInfo) {
        return mergeFrom((io.harness.event.payloads.EcsContainerInstanceInfo) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.event.payloads.EcsContainerInstanceInfo other) {
      if (other == io.harness.event.payloads.EcsContainerInstanceInfo.getDefaultInstance())
        return this;
      if (other.hasEcsContainerInstanceDescription()) {
        mergeEcsContainerInstanceDescription(other.getEcsContainerInstanceDescription());
      }
      if (other.hasEcsContainerInstanceResource()) {
        mergeEcsContainerInstanceResource(other.getEcsContainerInstanceResource());
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
      io.harness.event.payloads.EcsContainerInstanceInfo parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.event.payloads.EcsContainerInstanceInfo) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private io.harness.event.payloads.EcsContainerInstanceDescription ecsContainerInstanceDescription_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.EcsContainerInstanceDescription,
        io.harness.event.payloads.EcsContainerInstanceDescription.Builder,
        io.harness.event.payloads.EcsContainerInstanceDescriptionOrBuilder> ecsContainerInstanceDescriptionBuilder_;
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     * @return Whether the ecsContainerInstanceDescription field is set.
     */
    public boolean hasEcsContainerInstanceDescription() {
      return ecsContainerInstanceDescriptionBuilder_ != null || ecsContainerInstanceDescription_ != null;
    }
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     * @return The ecsContainerInstanceDescription.
     */
    public io.harness.event.payloads.EcsContainerInstanceDescription getEcsContainerInstanceDescription() {
      if (ecsContainerInstanceDescriptionBuilder_ == null) {
        return ecsContainerInstanceDescription_ == null
            ? io.harness.event.payloads.EcsContainerInstanceDescription.getDefaultInstance()
            : ecsContainerInstanceDescription_;
      } else {
        return ecsContainerInstanceDescriptionBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     */
    public Builder setEcsContainerInstanceDescription(io.harness.event.payloads.EcsContainerInstanceDescription value) {
      if (ecsContainerInstanceDescriptionBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ecsContainerInstanceDescription_ = value;
        onChanged();
      } else {
        ecsContainerInstanceDescriptionBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     */
    public Builder setEcsContainerInstanceDescription(
        io.harness.event.payloads.EcsContainerInstanceDescription.Builder builderForValue) {
      if (ecsContainerInstanceDescriptionBuilder_ == null) {
        ecsContainerInstanceDescription_ = builderForValue.build();
        onChanged();
      } else {
        ecsContainerInstanceDescriptionBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     */
    public Builder mergeEcsContainerInstanceDescription(
        io.harness.event.payloads.EcsContainerInstanceDescription value) {
      if (ecsContainerInstanceDescriptionBuilder_ == null) {
        if (ecsContainerInstanceDescription_ != null) {
          ecsContainerInstanceDescription_ =
              io.harness.event.payloads.EcsContainerInstanceDescription.newBuilder(ecsContainerInstanceDescription_)
                  .mergeFrom(value)
                  .buildPartial();
        } else {
          ecsContainerInstanceDescription_ = value;
        }
        onChanged();
      } else {
        ecsContainerInstanceDescriptionBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     */
    public Builder clearEcsContainerInstanceDescription() {
      if (ecsContainerInstanceDescriptionBuilder_ == null) {
        ecsContainerInstanceDescription_ = null;
        onChanged();
      } else {
        ecsContainerInstanceDescription_ = null;
        ecsContainerInstanceDescriptionBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     */
    public io.harness.event.payloads.EcsContainerInstanceDescription.Builder
    getEcsContainerInstanceDescriptionBuilder() {
      onChanged();
      return getEcsContainerInstanceDescriptionFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     */
    public io.harness.event.payloads.EcsContainerInstanceDescriptionOrBuilder
    getEcsContainerInstanceDescriptionOrBuilder() {
      if (ecsContainerInstanceDescriptionBuilder_ != null) {
        return ecsContainerInstanceDescriptionBuilder_.getMessageOrBuilder();
      } else {
        return ecsContainerInstanceDescription_ == null
            ? io.harness.event.payloads.EcsContainerInstanceDescription.getDefaultInstance()
            : ecsContainerInstanceDescription_;
      }
    }
    /**
     * <code>.io.harness.event.payloads.EcsContainerInstanceDescription ecs_container_instance_description = 1[json_name
     * = "ecsContainerInstanceDescription"];</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.EcsContainerInstanceDescription,
        io.harness.event.payloads.EcsContainerInstanceDescription.Builder,
        io.harness.event.payloads.EcsContainerInstanceDescriptionOrBuilder>
    getEcsContainerInstanceDescriptionFieldBuilder() {
      if (ecsContainerInstanceDescriptionBuilder_ == null) {
        ecsContainerInstanceDescriptionBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.EcsContainerInstanceDescription,
                io.harness.event.payloads.EcsContainerInstanceDescription.Builder,
                io.harness.event.payloads.EcsContainerInstanceDescriptionOrBuilder>(
                getEcsContainerInstanceDescription(), getParentForChildren(), isClean());
        ecsContainerInstanceDescription_ = null;
      }
      return ecsContainerInstanceDescriptionBuilder_;
    }

    private io.harness.event.payloads.ReservedResource ecsContainerInstanceResource_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.ReservedResource,
        io.harness.event.payloads.ReservedResource.Builder, io.harness.event.payloads.ReservedResourceOrBuilder>
        ecsContainerInstanceResourceBuilder_;
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     * @return Whether the ecsContainerInstanceResource field is set.
     */
    public boolean hasEcsContainerInstanceResource() {
      return ecsContainerInstanceResourceBuilder_ != null || ecsContainerInstanceResource_ != null;
    }
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     * @return The ecsContainerInstanceResource.
     */
    public io.harness.event.payloads.ReservedResource getEcsContainerInstanceResource() {
      if (ecsContainerInstanceResourceBuilder_ == null) {
        return ecsContainerInstanceResource_ == null ? io.harness.event.payloads.ReservedResource.getDefaultInstance()
                                                     : ecsContainerInstanceResource_;
      } else {
        return ecsContainerInstanceResourceBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     */
    public Builder setEcsContainerInstanceResource(io.harness.event.payloads.ReservedResource value) {
      if (ecsContainerInstanceResourceBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ecsContainerInstanceResource_ = value;
        onChanged();
      } else {
        ecsContainerInstanceResourceBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     */
    public Builder setEcsContainerInstanceResource(io.harness.event.payloads.ReservedResource.Builder builderForValue) {
      if (ecsContainerInstanceResourceBuilder_ == null) {
        ecsContainerInstanceResource_ = builderForValue.build();
        onChanged();
      } else {
        ecsContainerInstanceResourceBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     */
    public Builder mergeEcsContainerInstanceResource(io.harness.event.payloads.ReservedResource value) {
      if (ecsContainerInstanceResourceBuilder_ == null) {
        if (ecsContainerInstanceResource_ != null) {
          ecsContainerInstanceResource_ =
              io.harness.event.payloads.ReservedResource.newBuilder(ecsContainerInstanceResource_)
                  .mergeFrom(value)
                  .buildPartial();
        } else {
          ecsContainerInstanceResource_ = value;
        }
        onChanged();
      } else {
        ecsContainerInstanceResourceBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     */
    public Builder clearEcsContainerInstanceResource() {
      if (ecsContainerInstanceResourceBuilder_ == null) {
        ecsContainerInstanceResource_ = null;
        onChanged();
      } else {
        ecsContainerInstanceResource_ = null;
        ecsContainerInstanceResourceBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     */
    public io.harness.event.payloads.ReservedResource.Builder getEcsContainerInstanceResourceBuilder() {
      onChanged();
      return getEcsContainerInstanceResourceFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     */
    public io.harness.event.payloads.ReservedResourceOrBuilder getEcsContainerInstanceResourceOrBuilder() {
      if (ecsContainerInstanceResourceBuilder_ != null) {
        return ecsContainerInstanceResourceBuilder_.getMessageOrBuilder();
      } else {
        return ecsContainerInstanceResource_ == null ? io.harness.event.payloads.ReservedResource.getDefaultInstance()
                                                     : ecsContainerInstanceResource_;
      }
    }
    /**
     * <code>.io.harness.event.payloads.ReservedResource ecs_container_instance_resource = 2[json_name =
     * "ecsContainerInstanceResource"];</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.ReservedResource,
        io.harness.event.payloads.ReservedResource.Builder, io.harness.event.payloads.ReservedResourceOrBuilder>
    getEcsContainerInstanceResourceFieldBuilder() {
      if (ecsContainerInstanceResourceBuilder_ == null) {
        ecsContainerInstanceResourceBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<io.harness.event.payloads.ReservedResource,
                io.harness.event.payloads.ReservedResource.Builder,
                io.harness.event.payloads.ReservedResourceOrBuilder>(
                getEcsContainerInstanceResource(), getParentForChildren(), isClean());
        ecsContainerInstanceResource_ = null;
      }
      return ecsContainerInstanceResourceBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.event.payloads.EcsContainerInstanceInfo)
  }

  // @@protoc_insertion_point(class_scope:io.harness.event.payloads.EcsContainerInstanceInfo)
  private static final io.harness.event.payloads.EcsContainerInstanceInfo DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.event.payloads.EcsContainerInstanceInfo();
  }

  public static io.harness.event.payloads.EcsContainerInstanceInfo getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<EcsContainerInstanceInfo> PARSER =
      new com.google.protobuf.AbstractParser<EcsContainerInstanceInfo>() {
        @java.lang.Override
        public EcsContainerInstanceInfo parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new EcsContainerInstanceInfo(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<EcsContainerInstanceInfo> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<EcsContainerInstanceInfo> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.event.payloads.EcsContainerInstanceInfo getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
