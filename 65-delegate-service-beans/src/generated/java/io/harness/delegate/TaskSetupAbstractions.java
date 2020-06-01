// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/task.proto

package io.harness.delegate;

/**
 * Protobuf type {@code io.harness.delegate.TaskSetupAbstractions}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:TaskSetupAbstractions.java.pb.meta")
public final class TaskSetupAbstractions extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.delegate.TaskSetupAbstractions)
    TaskSetupAbstractionsOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use TaskSetupAbstractions.newBuilder() to construct.
  private TaskSetupAbstractions(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private TaskSetupAbstractions() {}

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new TaskSetupAbstractions();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private TaskSetupAbstractions(
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
              values_ = com.google.protobuf.MapField.newMapField(ValuesDefaultEntryHolder.defaultEntry);
              mutable_bitField0_ |= 0x00000001;
            }
            com.google.protobuf.MapEntry<java.lang.String, java.lang.String> values__ =
                input.readMessage(ValuesDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
            values_.getMutableMap().put(values__.getKey(), values__.getValue());
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
    return io.harness.delegate.Task.internal_static_io_harness_delegate_TaskSetupAbstractions_descriptor;
  }

  @SuppressWarnings({"rawtypes"})
  @java.
  lang.Override
  protected com.google.protobuf.MapField internalGetMapField(int number) {
    switch (number) {
      case 1:
        return internalGetValues();
      default:
        throw new RuntimeException("Invalid map field number: " + number);
    }
  }
  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.delegate.Task.internal_static_io_harness_delegate_TaskSetupAbstractions_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            io.harness.delegate.TaskSetupAbstractions.class, io.harness.delegate.TaskSetupAbstractions.Builder.class);
  }

  public static final int VALUES_FIELD_NUMBER = 1;
  private static final class ValuesDefaultEntryHolder {
    static final com.google.protobuf.MapEntry<java.lang.String, java.lang.String> defaultEntry =
        com.google.protobuf.MapEntry.<java.lang.String, java.lang.String>newDefaultInstance(
            io.harness.delegate.Task.internal_static_io_harness_delegate_TaskSetupAbstractions_ValuesEntry_descriptor,
            com.google.protobuf.WireFormat.FieldType.STRING, "", com.google.protobuf.WireFormat.FieldType.STRING, "");
  }
  private com.google.protobuf.MapField<java.lang.String, java.lang.String> values_;
  private com.google.protobuf.MapField<java.lang.String, java.lang.String> internalGetValues() {
    if (values_ == null) {
      return com.google.protobuf.MapField.emptyMapField(ValuesDefaultEntryHolder.defaultEntry);
    }
    return values_;
  }

  public int getValuesCount() {
    return internalGetValues().getMap().size();
  }
  /**
   * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
   */

  public boolean containsValues(java.lang.String key) {
    if (key == null) {
      throw new java.lang.NullPointerException();
    }
    return internalGetValues().getMap().containsKey(key);
  }
  /**
   * Use {@link #getValuesMap()} instead.
   */
  @java.
  lang.Deprecated
  public java.util.Map<java.lang.String, java.lang.String> getValues() {
    return getValuesMap();
  }
  /**
   * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
   */

  public java.util.Map<java.lang.String, java.lang.String> getValuesMap() {
    return internalGetValues().getMap();
  }
  /**
   * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
   */

  public java.lang.String getValuesOrDefault(java.lang.String key, java.lang.String defaultValue) {
    if (key == null) {
      throw new java.lang.NullPointerException();
    }
    java.util.Map<java.lang.String, java.lang.String> map = internalGetValues().getMap();
    return map.containsKey(key) ? map.get(key) : defaultValue;
  }
  /**
   * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
   */

  public java.lang.String getValuesOrThrow(java.lang.String key) {
    if (key == null) {
      throw new java.lang.NullPointerException();
    }
    java.util.Map<java.lang.String, java.lang.String> map = internalGetValues().getMap();
    if (!map.containsKey(key)) {
      throw new java.lang.IllegalArgumentException();
    }
    return map.get(key);
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
    com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(
        output, internalGetValues(), ValuesDefaultEntryHolder.defaultEntry, 1);
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    for (java.util.Map.Entry<java.lang.String, java.lang.String> entry : internalGetValues().getMap().entrySet()) {
      com.google.protobuf.MapEntry<java.lang.String, java.lang.String> values__ =
          ValuesDefaultEntryHolder.defaultEntry.newBuilderForType()
              .setKey(entry.getKey())
              .setValue(entry.getValue())
              .build();
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, values__);
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
    if (!(obj instanceof io.harness.delegate.TaskSetupAbstractions)) {
      return super.equals(obj);
    }
    io.harness.delegate.TaskSetupAbstractions other = (io.harness.delegate.TaskSetupAbstractions) obj;

    if (!internalGetValues().equals(other.internalGetValues()))
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
    if (!internalGetValues().getMap().isEmpty()) {
      hash = (37 * hash) + VALUES_FIELD_NUMBER;
      hash = (53 * hash) + internalGetValues().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.delegate.TaskSetupAbstractions parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.TaskSetupAbstractions parseFrom(com.google.protobuf.CodedInputStream input,
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
  public static Builder newBuilder(io.harness.delegate.TaskSetupAbstractions prototype) {
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
   * Protobuf type {@code io.harness.delegate.TaskSetupAbstractions}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.delegate.TaskSetupAbstractions)
      io.harness.delegate.TaskSetupAbstractionsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.delegate.Task.internal_static_io_harness_delegate_TaskSetupAbstractions_descriptor;
    }

    @SuppressWarnings({"rawtypes"})
    protected com.google.protobuf.MapField internalGetMapField(int number) {
      switch (number) {
        case 1:
          return internalGetValues();
        default:
          throw new RuntimeException("Invalid map field number: " + number);
      }
    }
    @SuppressWarnings({"rawtypes"})
    protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
      switch (number) {
        case 1:
          return internalGetMutableValues();
        default:
          throw new RuntimeException("Invalid map field number: " + number);
      }
    }
    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.delegate.Task.internal_static_io_harness_delegate_TaskSetupAbstractions_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.harness.delegate.TaskSetupAbstractions.class, io.harness.delegate.TaskSetupAbstractions.Builder.class);
    }

    // Construct using io.harness.delegate.TaskSetupAbstractions.newBuilder()
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
      internalGetMutableValues().clear();
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.delegate.Task.internal_static_io_harness_delegate_TaskSetupAbstractions_descriptor;
    }

    @java.
    lang.Override
    public io.harness.delegate.TaskSetupAbstractions getDefaultInstanceForType() {
      return io.harness.delegate.TaskSetupAbstractions.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.delegate.TaskSetupAbstractions build() {
      io.harness.delegate.TaskSetupAbstractions result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.delegate.TaskSetupAbstractions buildPartial() {
      io.harness.delegate.TaskSetupAbstractions result = new io.harness.delegate.TaskSetupAbstractions(this);
      int from_bitField0_ = bitField0_;
      result.values_ = internalGetValues();
      result.values_.makeImmutable();
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
      if (other instanceof io.harness.delegate.TaskSetupAbstractions) {
        return mergeFrom((io.harness.delegate.TaskSetupAbstractions) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.delegate.TaskSetupAbstractions other) {
      if (other == io.harness.delegate.TaskSetupAbstractions.getDefaultInstance())
        return this;
      internalGetMutableValues().mergeFrom(other.internalGetValues());
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
      io.harness.delegate.TaskSetupAbstractions parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.delegate.TaskSetupAbstractions) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private com.google.protobuf.MapField<java.lang.String, java.lang.String> values_;
    private com.google.protobuf.MapField<java.lang.String, java.lang.String> internalGetValues() {
      if (values_ == null) {
        return com.google.protobuf.MapField.emptyMapField(ValuesDefaultEntryHolder.defaultEntry);
      }
      return values_;
    }
    private com.google.protobuf.MapField<java.lang.String, java.lang.String> internalGetMutableValues() {
      onChanged();
      ;
      if (values_ == null) {
        values_ = com.google.protobuf.MapField.newMapField(ValuesDefaultEntryHolder.defaultEntry);
      }
      if (!values_.isMutable()) {
        values_ = values_.copy();
      }
      return values_;
    }

    public int getValuesCount() {
      return internalGetValues().getMap().size();
    }
    /**
     * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
     */

    public boolean containsValues(java.lang.String key) {
      if (key == null) {
        throw new java.lang.NullPointerException();
      }
      return internalGetValues().getMap().containsKey(key);
    }
    /**
     * Use {@link #getValuesMap()} instead.
     */
    @java.
    lang.Deprecated
    public java.util.Map<java.lang.String, java.lang.String> getValues() {
      return getValuesMap();
    }
    /**
     * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
     */

    public java.util.Map<java.lang.String, java.lang.String> getValuesMap() {
      return internalGetValues().getMap();
    }
    /**
     * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
     */

    public java.lang.String getValuesOrDefault(java.lang.String key, java.lang.String defaultValue) {
      if (key == null) {
        throw new java.lang.NullPointerException();
      }
      java.util.Map<java.lang.String, java.lang.String> map = internalGetValues().getMap();
      return map.containsKey(key) ? map.get(key) : defaultValue;
    }
    /**
     * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
     */

    public java.lang.String getValuesOrThrow(java.lang.String key) {
      if (key == null) {
        throw new java.lang.NullPointerException();
      }
      java.util.Map<java.lang.String, java.lang.String> map = internalGetValues().getMap();
      if (!map.containsKey(key)) {
        throw new java.lang.IllegalArgumentException();
      }
      return map.get(key);
    }

    public Builder clearValues() {
      internalGetMutableValues().getMutableMap().clear();
      return this;
    }
    /**
     * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
     */

    public Builder removeValues(java.lang.String key) {
      if (key == null) {
        throw new java.lang.NullPointerException();
      }
      internalGetMutableValues().getMutableMap().remove(key);
      return this;
    }
    /**
     * Use alternate mutation accessors instead.
     */
    @java.
    lang.Deprecated
    public java.util.Map<java.lang.String, java.lang.String> getMutableValues() {
      return internalGetMutableValues().getMutableMap();
    }
    /**
     * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
     */
    public Builder putValues(java.lang.String key, java.lang.String value) {
      if (key == null) {
        throw new java.lang.NullPointerException();
      }
      if (value == null) {
        throw new java.lang.NullPointerException();
      }
      internalGetMutableValues().getMutableMap().put(key, value);
      return this;
    }
    /**
     * <code>map&lt;string, string&gt; values = 1[json_name = "values"];</code>
     */

    public Builder putAllValues(java.util.Map<java.lang.String, java.lang.String> values) {
      internalGetMutableValues().getMutableMap().putAll(values);
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

    // @@protoc_insertion_point(builder_scope:io.harness.delegate.TaskSetupAbstractions)
  }

  // @@protoc_insertion_point(class_scope:io.harness.delegate.TaskSetupAbstractions)
  private static final io.harness.delegate.TaskSetupAbstractions DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.delegate.TaskSetupAbstractions();
  }

  public static io.harness.delegate.TaskSetupAbstractions getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<TaskSetupAbstractions> PARSER =
      new com.google.protobuf.AbstractParser<TaskSetupAbstractions>() {
        @java.lang.Override
        public TaskSetupAbstractions parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new TaskSetupAbstractions(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<TaskSetupAbstractions> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<TaskSetupAbstractions> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.delegate.TaskSetupAbstractions getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
