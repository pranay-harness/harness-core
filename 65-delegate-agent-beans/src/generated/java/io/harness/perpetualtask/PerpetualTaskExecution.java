// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task_execution.proto

package io.harness.perpetualtask;

@javax.annotation.Generated(value = "protoc", comments = "annotations:PerpetualTaskExecution.java.pb.meta")
public final class PerpetualTaskExecution {
  private PerpetualTaskExecution() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskExecutionParams_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskExecutionParams_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskAssignDetails_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskAssignDetails_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_PerpetualTaskExecutionContext_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_PerpetualTaskExecutionContext_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n7io/harness/perpetualtask/perpetual_tas"
        + "k_execution.proto\022\030io.harness.perpetualt"
        + "ask\032\031google/protobuf/any.proto\032\037google/p"
        + "rotobuf/timestamp.proto\032-io/harness/perp"
        + "etualtask/perpetual_task.proto\"O\n\034Perpet"
        + "ualTaskExecutionParams\022/\n\021customized_par"
        + "ams\030\001 \001(\0132\024.google.protobuf.Any\"\222\001\n\032Perp"
        + "etualTaskAssignDetails\022:\n\007task_id\030\001 \001(\0132"
        + ").io.harness.perpetualtask.PerpetualTask"
        + "Id\0228\n\024last_context_updated\030\002 \001(\0132\032.googl"
        + "e.protobuf.Timestamp\"\355\001\n\035PerpetualTaskEx"
        + "ecutionContext\022K\n\013task_params\030\001 \001(\01326.io"
        + ".harness.perpetualtask.PerpetualTaskExec"
        + "utionParams\022F\n\rtask_schedule\030\002 \001(\0132/.io."
        + "harness.perpetualtask.PerpetualTaskSched"
        + "ule\0227\n\023heartbeat_timestamp\030\003 \001(\0132\032.googl"
        + "e.protobuf.TimestampB\002P\001b\006proto3"};
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
            com.google.protobuf.TimestampProto.getDescriptor(),
            io.harness.perpetualtask.PerpetualTask.getDescriptor(),
        },
        assigner);
    internal_static_io_harness_perpetualtask_PerpetualTaskExecutionParams_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_perpetualtask_PerpetualTaskExecutionParams_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskExecutionParams_descriptor,
            new java.lang.String[] {
                "CustomizedParams",
            });
    internal_static_io_harness_perpetualtask_PerpetualTaskAssignDetails_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_perpetualtask_PerpetualTaskAssignDetails_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskAssignDetails_descriptor,
            new java.lang.String[] {
                "TaskId",
                "LastContextUpdated",
            });
    internal_static_io_harness_perpetualtask_PerpetualTaskExecutionContext_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_perpetualtask_PerpetualTaskExecutionContext_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_PerpetualTaskExecutionContext_descriptor,
            new java.lang.String[] {
                "TaskParams",
                "TaskSchedule",
                "HeartbeatTimestamp",
            });
    com.google.protobuf.AnyProto.getDescriptor();
    com.google.protobuf.TimestampProto.getDescriptor();
    io.harness.perpetualtask.PerpetualTask.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
