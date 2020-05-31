// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task_client_service.proto

package io.harness.perpetualtask;

@javax.annotation.
Generated(value = "protoc", comments = "annotations:PerpetualTaskClientServiceOuterClass.java.pb.meta")
public final class PerpetualTaskClientServiceOuterClass {
  private PerpetualTaskClientServiceOuterClass() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n<io/harness/perpetualtask/perpetual_tas"
        + "k_client_service.proto\022\030io.harness.perpe"
        + "tualtask\032\031google/protobuf/any.proto\032\037goo"
        + "gle/protobuf/timestamp.proto\032$io/harness"
        + "/delegate/capability.proto\032-io/harness/p"
        + "erpetualtask/perpetual_task.proto\"[\n\035Obt"
        + "ainTaskCapabilitiesRequest\022:\n\007task_id\030\001 "
        + "\001(\0132).io.harness.perpetualtask.Perpetual"
        + "TaskId\"W\n\036ObtainTaskCapabilitiesResponse"
        + "\0225\n\014capabilities\030\001 \003(\0132\037.io.harness.dele"
        + "gate.Capability\"^\n ObtainTaskExecutionPa"
        + "ramsRequest\022:\n\007task_id\030\001 \001(\0132).io.harnes"
        + "s.perpetualtask.PerpetualTaskId\"T\n!Obtai"
        + "nTaskExecutionParamsResponse\022/\n\021customiz"
        + "ed_params\030\001 \001(\0132\024.google.protobuf.Any2\301\002"
        + "\n\032PerpetualTaskClientService\022\213\001\n\026ObtainT"
        + "askCapabilities\0227.io.harness.perpetualta"
        + "sk.ObtainTaskCapabilitiesRequest\0328.io.ha"
        + "rness.perpetualtask.ObtainTaskCapabiliti"
        + "esResponse\022\224\001\n\031ObtainTaskExecutionParams"
        + "\022:.io.harness.perpetualtask.ObtainTaskEx"
        + "ecutionParamsRequest\032;.io.harness.perpet"
        + "ualtask.ObtainTaskExecutionParamsRespons"
        + "eB\002P\001b\006proto3"};
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
            io.harness.delegate.CapabilityOuterClass.getDescriptor(),
            io.harness.perpetualtask.PerpetualTask.getDescriptor(),
        },
        assigner);
    internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesRequest_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesResponse_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_ObtainTaskCapabilitiesResponse_descriptor,
            new java.lang.String[] {
                "Capabilities",
            });
    internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsRequest_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsResponse_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_ObtainTaskExecutionParamsResponse_descriptor,
            new java.lang.String[] {
                "CustomizedParams",
            });
    com.google.protobuf.AnyProto.getDescriptor();
    com.google.protobuf.TimestampProto.getDescriptor();
    io.harness.delegate.CapabilityOuterClass.getDescriptor();
    io.harness.perpetualtask.PerpetualTask.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
