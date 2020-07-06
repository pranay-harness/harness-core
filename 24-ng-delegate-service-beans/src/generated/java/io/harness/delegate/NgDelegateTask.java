// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/ng_delegate_task.proto

package io.harness.delegate;

@javax.annotation.Generated(value = "protoc", comments = "annotations:NgDelegateTask.java.pb.meta")
public final class NgDelegateTask {
  private NgDelegateTask() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SendTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SendTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SendTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SendTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SendTaskAsyncRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SendTaskAsyncRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SendTaskAsyncResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SendTaskAsyncResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_AbortTaskRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_AbortTaskRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_AbortTaskResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_AbortTaskResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SendTaskResultRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SendTaskResultRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_delegate_SendTaskResultResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_SendTaskResultResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n*io/harness/delegate/ng_delegate_task.p"
        + "roto\022\023io.harness.delegate\032\031google/protob"
        + "uf/any.proto\032!io/harness/delegate/accoun"
        + "t.proto\032\036io/harness/delegate/task.proto\032"
        + "\"io/harness/delegate/progress.proto\"\355\001\n\017"
        + "SendTaskRequest\022=\n\naccount_id\030\002 \001(\0132\036.io"
        + ".harness.delegate.AccountIdR\taccountId\022Y"
        + "\n\022setup_abstractions\030\003 \001(\0132*.io.harness."
        + "delegate.TaskSetupAbstractionsR\021setupAbs"
        + "tractions\022:\n\007details\030\004 \001(\0132 .io.harness."
        + "delegate.TaskDetailsR\007detailsJ\004\010\001\020\002\"m\n\020S"
        + "endTaskResponse\0224\n\007task_id\030\001 \001(\0132\033.io.ha"
        + "rness.delegate.TaskIdR\006taskId\022#\n\rrespons"
        + "e_data\030\002 \001(\014R\014responseData\"L\n\024SendTaskAs"
        + "yncRequest\0224\n\007task_id\030\001 \001(\0132\033.io.harness"
        + ".delegate.TaskIdR\006taskId\"M\n\025SendTaskAsyn"
        + "cResponse\0224\n\007task_id\030\001 \001(\0132\033.io.harness."
        + "delegate.TaskIdR\006taskId\"\207\001\n\020AbortTaskReq"
        + "uest\022=\n\naccount_id\030\001 \001(\0132\036.io.harness.de"
        + "legate.AccountIdR\taccountId\0224\n\007task_id\030\002"
        + " \001(\0132\033.io.harness.delegate.TaskIdR\006taskI"
        + "d\"h\n\021AbortTaskResponse\022S\n\021canceled_at_st"
        + "age\030\001 \001(\0162\'.io.harness.delegate.TaskExec"
        + "utionStageR\017canceledAtStage\"M\n\025SendTaskR"
        + "esultRequest\0224\n\007task_id\030\001 \001(\0132\033.io.harne"
        + "ss.delegate.TaskIdR\006taskId\"N\n\026SendTaskRe"
        + "sultResponse\0224\n\007task_id\030\001 \001(\0132\033.io.harne"
        + "ss.delegate.TaskIdR\006taskIdB\002P\001b\006proto3"};
    descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
            com.google.protobuf.AnyProto.getDescriptor(),
            io.harness.delegate.Account.getDescriptor(),
            io.harness.delegate.Task.getDescriptor(),
            io.harness.delegate.Progress.getDescriptor(),
        });
    internal_static_io_harness_delegate_SendTaskRequest_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_delegate_SendTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SendTaskRequest_descriptor,
            new java.lang.String[] {
                "AccountId",
                "SetupAbstractions",
                "Details",
            });
    internal_static_io_harness_delegate_SendTaskResponse_descriptor = getDescriptor().getMessageTypes().get(1);
    internal_static_io_harness_delegate_SendTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SendTaskResponse_descriptor,
            new java.lang.String[] {
                "TaskId",
                "ResponseData",
            });
    internal_static_io_harness_delegate_SendTaskAsyncRequest_descriptor = getDescriptor().getMessageTypes().get(2);
    internal_static_io_harness_delegate_SendTaskAsyncRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SendTaskAsyncRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_SendTaskAsyncResponse_descriptor = getDescriptor().getMessageTypes().get(3);
    internal_static_io_harness_delegate_SendTaskAsyncResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SendTaskAsyncResponse_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_AbortTaskRequest_descriptor = getDescriptor().getMessageTypes().get(4);
    internal_static_io_harness_delegate_AbortTaskRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_AbortTaskRequest_descriptor,
            new java.lang.String[] {
                "AccountId",
                "TaskId",
            });
    internal_static_io_harness_delegate_AbortTaskResponse_descriptor = getDescriptor().getMessageTypes().get(5);
    internal_static_io_harness_delegate_AbortTaskResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_AbortTaskResponse_descriptor,
            new java.lang.String[] {
                "CanceledAtStage",
            });
    internal_static_io_harness_delegate_SendTaskResultRequest_descriptor = getDescriptor().getMessageTypes().get(6);
    internal_static_io_harness_delegate_SendTaskResultRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SendTaskResultRequest_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    internal_static_io_harness_delegate_SendTaskResultResponse_descriptor = getDescriptor().getMessageTypes().get(7);
    internal_static_io_harness_delegate_SendTaskResultResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_SendTaskResultResponse_descriptor,
            new java.lang.String[] {
                "TaskId",
            });
    com.google.protobuf.AnyProto.getDescriptor();
    io.harness.delegate.Account.getDescriptor();
    io.harness.delegate.Task.getDescriptor();
    io.harness.delegate.Progress.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
