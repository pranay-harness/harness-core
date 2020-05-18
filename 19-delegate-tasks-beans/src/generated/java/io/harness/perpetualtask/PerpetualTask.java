// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task.proto

package io.harness.perpetualtask;

@javax.annotation.Generated(value = "protoc", comments = "annotations:PerpetualTask.java.pb.meta")
public final class PerpetualTask {
  private PerpetualTask() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskId_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskId_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskParams_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskParams_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskSchedule_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskSchedule_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskContext_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskContext_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n-io/harness/perpetualtask/perpetual_tas"
        + "k.proto\022\030io.harness.perpetualtask\032\031googl"
        + "e/protobuf/any.proto\032\036google/protobuf/du"
        + "ration.proto\032\037google/protobuf/timestamp."
        + "proto\"\035\n\017PerpetualTaskId\022\n\n\002id\030\001 \001(\t\"F\n\023"
        + "PerpetualTaskParams\022/\n\021customized_params"
        + "\030\001 \001(\0132\024.google.protobuf.Any\"p\n\025Perpetua"
        + "lTaskSchedule\022+\n\010interval\030\001 \001(\0132\031.google"
        + ".protobuf.Duration\022*\n\007timeout\030\002 \001(\0132\031.go"
        + "ogle.protobuf.Duration\"\333\001\n\024PerpetualTask"
        + "Context\022B\n\013task_params\030\001 \001(\0132-.io.harnes"
        + "s.perpetualtask.PerpetualTaskParams\022F\n\rt"
        + "ask_schedule\030\002 \001(\0132/.io.harness.perpetua"
        + "ltask.PerpetualTaskSchedule\0227\n\023heartbeat"
        + "_timestamp\030\003 \001(\0132\032.google.protobuf.Times"
        + "tampB\002P\001b\006proto3"};
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
            com.google.protobuf.AnyProto.getDescriptor(),
            com.google.protobuf.DurationProto.getDescriptor(),
            com.google.protobuf.TimestampProto.getDescriptor(),
        },
        assigner);
    internal_static_io_harness_perpetualtask_PerpetualTaskId_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_perpetualtask_PerpetualTaskId_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskId_descriptor,
            new java.lang.String[] {
                "Id",
            });
    internal_static_io_harness_perpetualtask_PerpetualTaskParams_descriptor = getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_perpetualtask_PerpetualTaskParams_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskParams_descriptor,
            new java.lang.String[] {
                "CustomizedParams",
            });
    internal_static_io_harness_perpetualtask_PerpetualTaskSchedule_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_perpetualtask_PerpetualTaskSchedule_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskSchedule_descriptor,
            new java.lang.String[] {
                "Interval",
                "Timeout",
            });
    internal_static_io_harness_perpetualtask_PerpetualTaskContext_descriptor = getDescriptor().getMessageTypes().get(3);
    internal_static_io_harness_perpetualtask_PerpetualTaskContext_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskContext_descriptor,
            new java.lang.String[] {
                "TaskParams",
                "TaskSchedule",
                "HeartbeatTimestamp",
            });
    com.google.protobuf.AnyProto.getDescriptor();
    com.google.protobuf.DurationProto.getDescriptor();
    com.google.protobuf.TimestampProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
