// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/delegate_service.proto

package io.harness.delegate;

@javax.annotation.Generated(value = "protoc", comments = "annotations:DelegateServiceOuterClass.java.pb.meta")
public final class DelegateServiceOuterClass {
  private DelegateServiceOuterClass() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SubmitTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SubmitTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SubmitTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SubmitTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_CancelTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_CancelTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_CancelTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_CancelTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskProgressRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskProgressRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskProgressResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskProgressResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskProgressUpdatesRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskProgressUpdatesRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_TaskProgressUpdatesResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_TaskProgressUpdatesResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_CreatePerpetualTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_CreatePerpetualTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_CreatePerpetualTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_CreatePerpetualTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_DeletePerpetualTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_DeletePerpetualTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_DeletePerpetualTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_DeletePerpetualTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_ResetPerpetualTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_ResetPerpetualTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_ResetPerpetualTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_ResetPerpetualTaskResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n*io/harness/delegate/delegate_service.p"
        + "roto\022\023io.harness.delegate\032!io/harness/de"
        + "legate/account.proto\032\036io/harness/delegat"
        + "e/task.proto\032\"io/harness/delegate/progre"
        + "ss.proto\032$io/harness/delegate/capability"
        + ".proto\032-io/harness/perpetualtask/perpetu"
        + "al_task.proto\0324io/harness/perpetualtask/"
        + "perpetual_task_client.proto\"\256\002\n\021SubmitTa"
        + "skRequest\022=\n\naccount_id\030\001 \001(\0132\036.io.harne"
        + "ss.delegate.AccountIdR\taccountId\022Y\n\022setu"
        + "p_abstractions\030\002 \001(\0132*.io.harness.delega"
        + "te.TaskSetupAbstractionsR\021setupAbstracti"
        + "ons\022:\n\007details\030\003 \001(\0132 .io.harness.delega"
        + "te.TaskDetailsR\007details\022C\n\014capabilities\030"
        + "\004 \003(\0132\037.io.harness.delegate.CapabilityR\014"
        + "capabilities\"J\n\022SubmitTaskResponse\0224\n\007ta"
        + "sk_id\030\001 \001(\0132\033.io.harness.delegate.TaskId"
        + "R\006taskId\"I\n\021CancelTaskRequest\0224\n\007task_id"
        + "\030\001 \001(\0132\033.io.harness.delegate.TaskIdR\006tas"
        + "kId\"i\n\022CancelTaskResponse\022S\n\021canceled_at"
        + "_stage\030\001 \001(\0162\'.io.harness.delegate.TaskE"
        + "xecutionStageR\017canceledAtStage\"K\n\023TaskPr"
        + "ogressRequest\0224\n\007task_id\030\001 \001(\0132\033.io.harn"
        + "ess.delegate.TaskIdR\006taskId\"m\n\024TaskProgr"
        + "essResponse\022U\n\022currently_at_stage\030\001 \001(\0162"
        + "\'.io.harness.delegate.TaskExecutionStage"
        + "R\020currentlyAtStage\"R\n\032TaskProgressUpdate"
        + "sRequest\0224\n\007task_id\030\001 \001(\0132\033.io.harness.d"
        + "elegate.TaskIdR\006taskId\"t\n\033TaskProgressUp"
        + "datesResponse\022U\n\022currently_at_stage\030\001 \001("
        + "\0162\'.io.harness.delegate.TaskExecutionSta"
        + "geR\020currentlyAtStage\"\305\001\n,RegisterPerpetu"
        + "alTaskClientEntrypointRequest\022\022\n\004type\030\001 "
        + "\001(\tR\004type\022\200\001\n perpetual_task_client_entr"
        + "ypoint\030\002 \001(\01327.io.harness.perpetualtask."
        + "PerpetualTaskClientEntrypointR\035perpetual"
        + "TaskClientEntrypoint\"/\n-RegisterPerpetua"
        + "lTaskClientEntrypointResponse\"\274\002\n\032Create"
        + "PerpetualTaskRequest\022=\n\naccount_id\030\001 \001(\013"
        + "2\036.io.harness.delegate.AccountIdR\taccoun"
        + "tId\022\022\n\004type\030\002 \001(\tR\004type\022K\n\010schedule\030\003 \001("
        + "\0132/.io.harness.perpetualtask.PerpetualTa"
        + "skScheduleR\010schedule\022U\n\007context\030\004 \001(\0132;."
        + "io.harness.perpetualtask.PerpetualTaskCl"
        + "ientContextDetailsR\007context\022\'\n\017allow_dup"
        + "licate\030\005 \001(\010R\016allowDuplicate\"t\n\033CreatePe"
        + "rpetualTaskResponse\022U\n\021perpetual_task_id"
        + "\030\001 \001(\0132).io.harness.perpetualtask.Perpet"
        + "ualTaskIdR\017perpetualTaskId\"\262\001\n\032DeletePer"
        + "petualTaskRequest\022=\n\naccount_id\030\001 \001(\0132\036."
        + "io.harness.delegate.AccountIdR\taccountId"
        + "\022U\n\021perpetual_task_id\030\002 \001(\0132).io.harness"
        + ".perpetualtask.PerpetualTaskIdR\017perpetua"
        + "lTaskId\"\035\n\033DeletePerpetualTaskResponse\"\261"
        + "\001\n\031ResetPerpetualTaskRequest\022=\n\naccount_"
        + "id\030\001 \001(\0132\036.io.harness.delegate.AccountId"
        + "R\taccountId\022U\n\021perpetual_task_id\030\002 \001(\0132)"
        + ".io.harness.perpetualtask.PerpetualTaskI"
        + "dR\017perpetualTaskId\"\034\n\032ResetPerpetualTask"
        + "Response2\314\007\n\017DelegateService\022]\n\nSubmitTa"
        + "sk\022&.io.harness.delegate.SubmitTaskReque"
        + "st\032\'.io.harness.delegate.SubmitTaskRespo"
        + "nse\022]\n\nCancelTask\022&.io.harness.delegate."
        + "CancelTaskRequest\032\'.io.harness.delegate."
        + "CancelTaskResponse\022c\n\014TaskProgress\022(.io."
        + "harness.delegate.TaskProgressRequest\032).i"
        + "o.harness.delegate.TaskProgressResponse\022"
        + "z\n\023TaskProgressUpdates\022/.io.harness.dele"
        + "gate.TaskProgressUpdatesRequest\0320.io.har"
        + "ness.delegate.TaskProgressUpdatesRespons"
        + "e0\001\022\256\001\n%RegisterPerpetualTaskClientEntry"
        + "point\022A.io.harness.delegate.RegisterPerp"
        + "etualTaskClientEntrypointRequest\032B.io.ha"
        + "rness.delegate.RegisterPerpetualTaskClie"
        + "ntEntrypointResponse\022x\n\023CreatePerpetualT"
        + "ask\022/.io.harness.delegate.CreatePerpetua"
        + "lTaskRequest\0320.io.harness.delegate.Creat"
        + "ePerpetualTaskResponse\022x\n\023DeletePerpetua"
        + "lTask\022/.io.harness.delegate.DeletePerpet"
        + "ualTaskRequest\0320.io.harness.delegate.Del"
        + "etePerpetualTaskResponse\022u\n\022ResetPerpetu"
        + "alTask\022..io.harness.delegate.ResetPerpet"
        + "ualTaskRequest\032/.io.harness.delegate.Res"
        + "etPerpetualTaskResponseB\002P\001b\006proto3"};
    descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
            io.harness.delegate.Account.getDescriptor(),
            io.harness.delegate.Task.getDescriptor(),
            io.harness.delegate.Progress.getDescriptor(),
            io.harness.delegate.CapabilityOuterClass.getDescriptor(),
            io.harness.perpetualtask.PerpetualTask.getDescriptor(),
            io.harness.perpetualtask.PerpetualTaskClient.getDescriptor(),
        });
    internal_static_io_harness_delegate_SubmitTaskRequest_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_delegate_SubmitTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SubmitTaskRequest_descriptor,
            new java.lang.String[] {
                "AccountId",
                "SetupAbstractions",
                "Details",
                "Capabilities",
            });
    internal_static_io_harness_delegate_SubmitTaskResponse_descriptor = getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_delegate_SubmitTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SubmitTaskResponse_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_CancelTaskRequest_descriptor = getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_delegate_CancelTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_CancelTaskRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_CancelTaskResponse_descriptor = getDescriptor().getMessageTypes().get(3);
    internal_static_io_harness_delegate_CancelTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_CancelTaskResponse_descriptor,
            new java.lang.String[] {
                "CanceledAtStage",
            });
    internal_static_io_harness_delegate_TaskProgressRequest_descriptor = getDescriptor().getMessageTypes().get(4);
    internal_static_io_harness_delegate_TaskProgressRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskProgressRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_TaskProgressResponse_descriptor = getDescriptor().getMessageTypes().get(5);
    internal_static_io_harness_delegate_TaskProgressResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskProgressResponse_descriptor,
            new java.lang.String[] {
                "CurrentlyAtStage",
            });
    internal_static_io_harness_delegate_TaskProgressUpdatesRequest_descriptor =
        getDescriptor().getMessageTypes().get(6);
    internal_static_io_harness_delegate_TaskProgressUpdatesRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskProgressUpdatesRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_TaskProgressUpdatesResponse_descriptor =
        getDescriptor().getMessageTypes().get(7);
    internal_static_io_harness_delegate_TaskProgressUpdatesResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_TaskProgressUpdatesResponse_descriptor,
            new java.lang.String[] {
                "CurrentlyAtStage",
            });
    internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_descriptor =
        getDescriptor().getMessageTypes().get(8);
    internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointRequest_descriptor,
            new java.lang.String[] {
                "Type",
                "PerpetualTaskClientEntrypoint",
            });
    internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_descriptor =
        getDescriptor().getMessageTypes().get(9);
    internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_RegisterPerpetualTaskClientEntrypointResponse_descriptor,
            new java.lang.String[] {});
    internal_static_io_harness_delegate_CreatePerpetualTaskRequest_descriptor =
        getDescriptor().getMessageTypes().get(10);
    internal_static_io_harness_delegate_CreatePerpetualTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_CreatePerpetualTaskRequest_descriptor,
            new java.lang.String[] {
                "AccountId",
                "Type",
                "Schedule",
                "Context",
                "AllowDuplicate",
            });
    internal_static_io_harness_delegate_CreatePerpetualTaskResponse_descriptor =
        getDescriptor().getMessageTypes().get(11);
    internal_static_io_harness_delegate_CreatePerpetualTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_CreatePerpetualTaskResponse_descriptor,
            new java.lang.String[] {
                "PerpetualTaskId",
            });
    internal_static_io_harness_delegate_DeletePerpetualTaskRequest_descriptor =
        getDescriptor().getMessageTypes().get(12);
    internal_static_io_harness_delegate_DeletePerpetualTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_DeletePerpetualTaskRequest_descriptor,
            new java.lang.String[] {
                "AccountId",
                "PerpetualTaskId",
            });
    internal_static_io_harness_delegate_DeletePerpetualTaskResponse_descriptor =
        getDescriptor().getMessageTypes().get(13);
    internal_static_io_harness_delegate_DeletePerpetualTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_DeletePerpetualTaskResponse_descriptor, new java.lang.String[] {});
    internal_static_io_harness_delegate_ResetPerpetualTaskRequest_descriptor =
        getDescriptor().getMessageTypes().get(14);
    internal_static_io_harness_delegate_ResetPerpetualTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_ResetPerpetualTaskRequest_descriptor,
            new java.lang.String[] {
                "AccountId",
                "PerpetualTaskId",
            });
    internal_static_io_harness_delegate_ResetPerpetualTaskResponse_descriptor =
        getDescriptor().getMessageTypes().get(15);
    internal_static_io_harness_delegate_ResetPerpetualTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_ResetPerpetualTaskResponse_descriptor, new java.lang.String[] {});
    io.harness.delegate.Account.getDescriptor();
    io.harness.delegate.Task.getDescriptor();
    io.harness.delegate.Progress.getDescriptor();
    io.harness.delegate.CapabilityOuterClass.getDescriptor();
    io.harness.perpetualtask.PerpetualTask.getDescriptor();
    io.harness.perpetualtask.PerpetualTaskClient.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
