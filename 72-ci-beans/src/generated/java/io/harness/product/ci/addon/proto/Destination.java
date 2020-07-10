// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/addon/proto/addon.proto

package io.harness.product.ci.addon.proto;

/**
 * Protobuf type {@code io.harness.product.ci.addon.proto.Destination}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:Destination.java.pb.meta")
public final class Destination extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.product.ci.addon.proto.Destination)
    DestinationOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use Destination.newBuilder() to construct.
  private Destination(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private Destination() {
    destinationUrl_ = "";
    locationType_ = 0;
    region_ = "";
  }

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new Destination();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private Destination(
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

            destinationUrl_ = s;
            break;
          }
          case 18: {
            io.harness.product.ci.addon.proto.Connector.Builder subBuilder = null;
            if (connector_ != null) {
              subBuilder = connector_.toBuilder();
            }
            connector_ = input.readMessage(io.harness.product.ci.addon.proto.Connector.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(connector_);
              connector_ = subBuilder.buildPartial();
            }

            break;
          }
          case 24: {
            int rawValue = input.readEnum();

            locationType_ = rawValue;
            break;
          }
          case 34: {
            java.lang.String s = input.readStringRequireUtf8();

            region_ = s;
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
        .internal_static_io_harness_product_ci_addon_proto_Destination_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.product.ci.addon.proto.Addon
        .internal_static_io_harness_product_ci_addon_proto_Destination_fieldAccessorTable
        .ensureFieldAccessorsInitialized(io.harness.product.ci.addon.proto.Destination.class,
            io.harness.product.ci.addon.proto.Destination.Builder.class);
  }

  public static final int DESTINATION_URL_FIELD_NUMBER = 1;
  private volatile java.lang.Object destinationUrl_;
  /**
   * <code>string destination_url = 1[json_name = "destinationUrl"];</code>
   * @return The destinationUrl.
   */
  public java.lang.String getDestinationUrl() {
    java.lang.Object ref = destinationUrl_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      destinationUrl_ = s;
      return s;
    }
  }
  /**
   * <code>string destination_url = 1[json_name = "destinationUrl"];</code>
   * @return The bytes for destinationUrl.
   */
  public com.google.protobuf.ByteString getDestinationUrlBytes() {
    java.lang.Object ref = destinationUrl_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      destinationUrl_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int CONNECTOR_FIELD_NUMBER = 2;
  private io.harness.product.ci.addon.proto.Connector connector_;
  /**
   * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
   * @return Whether the connector field is set.
   */
  public boolean hasConnector() {
    return connector_ != null;
  }
  /**
   * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
   * @return The connector.
   */
  public io.harness.product.ci.addon.proto.Connector getConnector() {
    return connector_ == null ? io.harness.product.ci.addon.proto.Connector.getDefaultInstance() : connector_;
  }
  /**
   * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
   */
  public io.harness.product.ci.addon.proto.ConnectorOrBuilder getConnectorOrBuilder() {
    return getConnector();
  }

  public static final int LOCATION_TYPE_FIELD_NUMBER = 3;
  private int locationType_;
  /**
   * <code>.io.harness.product.ci.addon.proto.LocationType location_type = 3[json_name = "locationType"];</code>
   * @return The enum numeric value on the wire for locationType.
   */
  public int getLocationTypeValue() {
    return locationType_;
  }
  /**
   * <code>.io.harness.product.ci.addon.proto.LocationType location_type = 3[json_name = "locationType"];</code>
   * @return The locationType.
   */
  public io.harness.product.ci.addon.proto.LocationType getLocationType() {
    @SuppressWarnings("deprecation")
    io.harness.product.ci.addon.proto.LocationType result =
        io.harness.product.ci.addon.proto.LocationType.valueOf(locationType_);
    return result == null ? io.harness.product.ci.addon.proto.LocationType.UNRECOGNIZED : result;
  }

  public static final int REGION_FIELD_NUMBER = 4;
  private volatile java.lang.Object region_;
  /**
   * <code>string region = 4[json_name = "region"];</code>
   * @return The region.
   */
  public java.lang.String getRegion() {
    java.lang.Object ref = region_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      region_ = s;
      return s;
    }
  }
  /**
   * <code>string region = 4[json_name = "region"];</code>
   * @return The bytes for region.
   */
  public com.google.protobuf.ByteString getRegionBytes() {
    java.lang.Object ref = region_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
      region_ = b;
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
    if (!getDestinationUrlBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, destinationUrl_);
    }
    if (connector_ != null) {
      output.writeMessage(2, getConnector());
    }
    if (locationType_ != io.harness.product.ci.addon.proto.LocationType.UNKNOWN.getNumber()) {
      output.writeEnum(3, locationType_);
    }
    if (!getRegionBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, region_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getDestinationUrlBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, destinationUrl_);
    }
    if (connector_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getConnector());
    }
    if (locationType_ != io.harness.product.ci.addon.proto.LocationType.UNKNOWN.getNumber()) {
      size += com.google.protobuf.CodedOutputStream.computeEnumSize(3, locationType_);
    }
    if (!getRegionBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, region_);
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
    if (!(obj instanceof io.harness.product.ci.addon.proto.Destination)) {
      return super.equals(obj);
    }
    io.harness.product.ci.addon.proto.Destination other = (io.harness.product.ci.addon.proto.Destination) obj;

    if (!getDestinationUrl().equals(other.getDestinationUrl()))
      return false;
    if (hasConnector() != other.hasConnector())
      return false;
    if (hasConnector()) {
      if (!getConnector().equals(other.getConnector()))
        return false;
    }
    if (locationType_ != other.locationType_)
      return false;
    if (!getRegion().equals(other.getRegion()))
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
    hash = (37 * hash) + DESTINATION_URL_FIELD_NUMBER;
    hash = (53 * hash) + getDestinationUrl().hashCode();
    if (hasConnector()) {
      hash = (37 * hash) + CONNECTOR_FIELD_NUMBER;
      hash = (53 * hash) + getConnector().hashCode();
    }
    hash = (37 * hash) + LOCATION_TYPE_FIELD_NUMBER;
    hash = (53 * hash) + locationType_;
    hash = (37 * hash) + REGION_FIELD_NUMBER;
    hash = (53 * hash) + getRegion().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.product.ci.addon.proto.Destination parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.Destination parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.addon.proto.Destination parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.product.ci.addon.proto.Destination parseFrom(com.google.protobuf.CodedInputStream input,
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
  public static Builder newBuilder(io.harness.product.ci.addon.proto.Destination prototype) {
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
   * Protobuf type {@code io.harness.product.ci.addon.proto.Destination}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.product.ci.addon.proto.Destination)
      io.harness.product.ci.addon.proto.DestinationOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.product.ci.addon.proto.Addon
          .internal_static_io_harness_product_ci_addon_proto_Destination_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.product.ci.addon.proto.Addon
          .internal_static_io_harness_product_ci_addon_proto_Destination_fieldAccessorTable
          .ensureFieldAccessorsInitialized(io.harness.product.ci.addon.proto.Destination.class,
              io.harness.product.ci.addon.proto.Destination.Builder.class);
    }

    // Construct using io.harness.product.ci.addon.proto.Destination.newBuilder()
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
      destinationUrl_ = "";

      if (connectorBuilder_ == null) {
        connector_ = null;
      } else {
        connector_ = null;
        connectorBuilder_ = null;
      }
      locationType_ = 0;

      region_ = "";

      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.product.ci.addon.proto.Addon
          .internal_static_io_harness_product_ci_addon_proto_Destination_descriptor;
    }

    @java.
    lang.Override
    public io.harness.product.ci.addon.proto.Destination getDefaultInstanceForType() {
      return io.harness.product.ci.addon.proto.Destination.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.product.ci.addon.proto.Destination build() {
      io.harness.product.ci.addon.proto.Destination result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.product.ci.addon.proto.Destination buildPartial() {
      io.harness.product.ci.addon.proto.Destination result = new io.harness.product.ci.addon.proto.Destination(this);
      result.destinationUrl_ = destinationUrl_;
      if (connectorBuilder_ == null) {
        result.connector_ = connector_;
      } else {
        result.connector_ = connectorBuilder_.build();
      }
      result.locationType_ = locationType_;
      result.region_ = region_;
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
      if (other instanceof io.harness.product.ci.addon.proto.Destination) {
        return mergeFrom((io.harness.product.ci.addon.proto.Destination) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.product.ci.addon.proto.Destination other) {
      if (other == io.harness.product.ci.addon.proto.Destination.getDefaultInstance())
        return this;
      if (!other.getDestinationUrl().isEmpty()) {
        destinationUrl_ = other.destinationUrl_;
        onChanged();
      }
      if (other.hasConnector()) {
        mergeConnector(other.getConnector());
      }
      if (other.locationType_ != 0) {
        setLocationTypeValue(other.getLocationTypeValue());
      }
      if (!other.getRegion().isEmpty()) {
        region_ = other.region_;
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
      io.harness.product.ci.addon.proto.Destination parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.product.ci.addon.proto.Destination) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object destinationUrl_ = "";
    /**
     * <code>string destination_url = 1[json_name = "destinationUrl"];</code>
     * @return The destinationUrl.
     */
    public java.lang.String getDestinationUrl() {
      java.lang.Object ref = destinationUrl_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        destinationUrl_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string destination_url = 1[json_name = "destinationUrl"];</code>
     * @return The bytes for destinationUrl.
     */
    public com.google.protobuf.ByteString getDestinationUrlBytes() {
      java.lang.Object ref = destinationUrl_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        destinationUrl_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string destination_url = 1[json_name = "destinationUrl"];</code>
     * @param value The destinationUrl to set.
     * @return This builder for chaining.
     */
    public Builder setDestinationUrl(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      destinationUrl_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string destination_url = 1[json_name = "destinationUrl"];</code>
     * @return This builder for chaining.
     */
    public Builder clearDestinationUrl() {
      destinationUrl_ = getDefaultInstance().getDestinationUrl();
      onChanged();
      return this;
    }
    /**
     * <code>string destination_url = 1[json_name = "destinationUrl"];</code>
     * @param value The bytes for destinationUrl to set.
     * @return This builder for chaining.
     */
    public Builder setDestinationUrlBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      destinationUrl_ = value;
      onChanged();
      return this;
    }

    private io.harness.product.ci.addon.proto.Connector connector_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.product.ci.addon.proto.Connector,
        io.harness.product.ci.addon.proto.Connector.Builder, io.harness.product.ci.addon.proto.ConnectorOrBuilder>
        connectorBuilder_;
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     * @return Whether the connector field is set.
     */
    public boolean hasConnector() {
      return connectorBuilder_ != null || connector_ != null;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     * @return The connector.
     */
    public io.harness.product.ci.addon.proto.Connector getConnector() {
      if (connectorBuilder_ == null) {
        return connector_ == null ? io.harness.product.ci.addon.proto.Connector.getDefaultInstance() : connector_;
      } else {
        return connectorBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     */
    public Builder setConnector(io.harness.product.ci.addon.proto.Connector value) {
      if (connectorBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        connector_ = value;
        onChanged();
      } else {
        connectorBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     */
    public Builder setConnector(io.harness.product.ci.addon.proto.Connector.Builder builderForValue) {
      if (connectorBuilder_ == null) {
        connector_ = builderForValue.build();
        onChanged();
      } else {
        connectorBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     */
    public Builder mergeConnector(io.harness.product.ci.addon.proto.Connector value) {
      if (connectorBuilder_ == null) {
        if (connector_ != null) {
          connector_ =
              io.harness.product.ci.addon.proto.Connector.newBuilder(connector_).mergeFrom(value).buildPartial();
        } else {
          connector_ = value;
        }
        onChanged();
      } else {
        connectorBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     */
    public Builder clearConnector() {
      if (connectorBuilder_ == null) {
        connector_ = null;
        onChanged();
      } else {
        connector_ = null;
        connectorBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     */
    public io.harness.product.ci.addon.proto.Connector.Builder getConnectorBuilder() {
      onChanged();
      return getConnectorFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     */
    public io.harness.product.ci.addon.proto.ConnectorOrBuilder getConnectorOrBuilder() {
      if (connectorBuilder_ != null) {
        return connectorBuilder_.getMessageOrBuilder();
      } else {
        return connector_ == null ? io.harness.product.ci.addon.proto.Connector.getDefaultInstance() : connector_;
      }
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.Connector connector = 2[json_name = "connector"];</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.product.ci.addon.proto.Connector,
        io.harness.product.ci.addon.proto.Connector.Builder, io.harness.product.ci.addon.proto.ConnectorOrBuilder>
    getConnectorFieldBuilder() {
      if (connectorBuilder_ == null) {
        connectorBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<io.harness.product.ci.addon.proto.Connector,
            io.harness.product.ci.addon.proto.Connector.Builder, io.harness.product.ci.addon.proto.ConnectorOrBuilder>(
            getConnector(), getParentForChildren(), isClean());
        connector_ = null;
      }
      return connectorBuilder_;
    }

    private int locationType_ = 0;
    /**
     * <code>.io.harness.product.ci.addon.proto.LocationType location_type = 3[json_name = "locationType"];</code>
     * @return The enum numeric value on the wire for locationType.
     */
    public int getLocationTypeValue() {
      return locationType_;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.LocationType location_type = 3[json_name = "locationType"];</code>
     * @param value The enum numeric value on the wire for locationType to set.
     * @return This builder for chaining.
     */
    public Builder setLocationTypeValue(int value) {
      locationType_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.LocationType location_type = 3[json_name = "locationType"];</code>
     * @return The locationType.
     */
    public io.harness.product.ci.addon.proto.LocationType getLocationType() {
      @SuppressWarnings("deprecation")
      io.harness.product.ci.addon.proto.LocationType result =
          io.harness.product.ci.addon.proto.LocationType.valueOf(locationType_);
      return result == null ? io.harness.product.ci.addon.proto.LocationType.UNRECOGNIZED : result;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.LocationType location_type = 3[json_name = "locationType"];</code>
     * @param value The locationType to set.
     * @return This builder for chaining.
     */
    public Builder setLocationType(io.harness.product.ci.addon.proto.LocationType value) {
      if (value == null) {
        throw new NullPointerException();
      }

      locationType_ = value.getNumber();
      onChanged();
      return this;
    }
    /**
     * <code>.io.harness.product.ci.addon.proto.LocationType location_type = 3[json_name = "locationType"];</code>
     * @return This builder for chaining.
     */
    public Builder clearLocationType() {
      locationType_ = 0;
      onChanged();
      return this;
    }

    private java.lang.Object region_ = "";
    /**
     * <code>string region = 4[json_name = "region"];</code>
     * @return The region.
     */
    public java.lang.String getRegion() {
      java.lang.Object ref = region_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        region_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string region = 4[json_name = "region"];</code>
     * @return The bytes for region.
     */
    public com.google.protobuf.ByteString getRegionBytes() {
      java.lang.Object ref = region_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);
        region_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string region = 4[json_name = "region"];</code>
     * @param value The region to set.
     * @return This builder for chaining.
     */
    public Builder setRegion(java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      region_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string region = 4[json_name = "region"];</code>
     * @return This builder for chaining.
     */
    public Builder clearRegion() {
      region_ = getDefaultInstance().getRegion();
      onChanged();
      return this;
    }
    /**
     * <code>string region = 4[json_name = "region"];</code>
     * @param value The bytes for region to set.
     * @return This builder for chaining.
     */
    public Builder setRegionBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      region_ = value;
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

    // @@protoc_insertion_point(builder_scope:io.harness.product.ci.addon.proto.Destination)
  }

  // @@protoc_insertion_point(class_scope:io.harness.product.ci.addon.proto.Destination)
  private static final io.harness.product.ci.addon.proto.Destination DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.product.ci.addon.proto.Destination();
  }

  public static io.harness.product.ci.addon.proto.Destination getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Destination> PARSER =
      new com.google.protobuf.AbstractParser<Destination>() {
        @java.lang.Override
        public Destination parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new Destination(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<Destination> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<Destination> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.product.ci.addon.proto.Destination getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
