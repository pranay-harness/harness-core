// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/ng_delegate_task_response_service.proto

package io.harness.delegate;

@javax.annotation.
Generated(value = "protoc", comments = "annotations:NgDelegateTaskResponseServiceOuterClass.java.pb.meta")
public final class NgDelegateTaskResponseServiceOuterClass {
  private NgDelegateTaskResponseServiceOuterClass() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n;io/harness/delegate/ng_delegate_task_r"
        + "esponse_service.proto\022\023io.harness.delega"
        + "te\0320io/harness/delegate/delegate_task_re"
        + "sponse.proto\032>io/harness/perpetualtask/n"
        + "g_perpetualtask_service_client.proto2\364\003\n"
        + "\035NgDelegateTaskResponseService\022i\n\016SendTa"
        + "skResult\022*.io.harness.delegate.SendTaskR"
        + "esultRequest\032+.io.harness.delegate.SendT"
        + "askResultResponse\022\265\001\n$ObtainPerpetualTas"
        + "kValidationDetails\022E.io.harness.perpetua"
        + "ltask.ObtainPerpetualTaskValidationDetai"
        + "lsRequest\032F.io.harness.perpetualtask.Obt"
        + "ainPerpetualTaskValidationDetailsRespons"
        + "e\022\257\001\n\"ObtainPerpetualTaskExecutionParams"
        + "\022C.io.harness.perpetualtask.ObtainPerpet"
        + "ualTaskExecutionParamsRequest\032D.io.harne"
        + "ss.perpetualtask.ObtainPerpetualTaskExec"
        + "utionParamsResponseB\002P\001b\006proto3"};
    descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
            io.harness.delegate.DelegateTaskResponse.getDescriptor(),
            io.harness.perpetualtask.NgPerpetualtaskServiceClient.getDescriptor(),
        });
    io.harness.delegate.DelegateTaskResponse.getDescriptor();
    io.harness.perpetualtask.NgPerpetualtaskServiceClient.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
