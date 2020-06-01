// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/progress.proto

package io.harness.delegate;

/**
 * Protobuf enum {@code io.harness.delegate.TaskExecutionStage}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:TaskExecutionStage.java.pb.meta")
public enum TaskExecutionStage implements com
.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>TYPE_UNSPECIFIED = 0;</code>
   */
  TYPE_UNSPECIFIED(0),
      /**
       * <code>QUEUEING = 1;</code>
       */
      QUEUEING(1),
      /**
       * <code>VALIDATING = 2;</code>
       */
      VALIDATING(2),
      /**
       * <code>EXECUTING = 3;</code>
       */
      EXECUTING(3),
      /**
       * <code>FINISHED = 4;</code>
       */
      FINISHED(4), UNRECOGNIZED(-1),
      ;

  /**
   * <code>TYPE_UNSPECIFIED = 0;</code>
   */
  public static final int TYPE_UNSPECIFIED_VALUE = 0;
  /**
   * <code>QUEUEING = 1;</code>
   */
  public static final int QUEUEING_VALUE = 1;
  /**
   * <code>VALIDATING = 2;</code>
   */
  public static final int VALIDATING_VALUE = 2;
  /**
   * <code>EXECUTING = 3;</code>
   */
  public static final int EXECUTING_VALUE = 3;
  /**
   * <code>FINISHED = 4;</code>
   */
  public static final int FINISHED_VALUE = 4;

  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException("Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static TaskExecutionStage valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static TaskExecutionStage forNumber(int value) {
    switch (value) {
      case 0:
        return TYPE_UNSPECIFIED;
      case 1:
        return QUEUEING;
      case 2:
        return VALIDATING;
      case 3:
        return EXECUTING;
      case 4:
        return FINISHED;
      default:
        return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<TaskExecutionStage> internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<TaskExecutionStage> internalValueMap =
      new com.google.protobuf.Internal.EnumLiteMap<TaskExecutionStage>() {
        public TaskExecutionStage findValueByNumber(int number) {
          return TaskExecutionStage.forNumber(number);
        }
      };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
    return io.harness.delegate.Progress.getDescriptor().getEnumTypes().get(0);
  }

  private static final TaskExecutionStage[] VALUES = values();

  public static TaskExecutionStage valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException("EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private TaskExecutionStage(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:io.harness.delegate.TaskExecutionStage)
}
