// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/datacollection/data_collection_task.proto

package io.harness.perpetualtask.datacollection;

@javax.annotation.Generated(value = "protoc", comments = "annotations:DataCollectionTask.java.pb.meta")
public final class DataCollectionTask {
  private DataCollectionTask() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors
      .Descriptor internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\nBio/harness/perpetualtask/datacollectio"
        + "n/data_collection_task.proto\022\'io.harness"
        + ".perpetualtask.datacollection\"\203\002\n!DataCo"
        + "llectionPerpetualTaskParams\022\035\n\naccount_i"
        + "d\030\001 \001(\tR\taccountId\022 \n\014cv_config_id\030\002 \001(\t"
        + "R\ncvConfigId\0220\n\024data_collection_info\030\003 \001"
        + "(\014R\022dataCollectionInfo\0220\n\024verification_t"
        + "ask_id\030\004 \001(\tR\022verificationTaskId\0229\n\031data"
        + "_collection_worker_id\030\005 \001(\tR\026dataCollect"
        + "ionWorkerIdB\002P\001b\006proto3"};
    descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
        descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {});
    internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_perpetualtask_datacollection_DataCollectionPerpetualTaskParams_descriptor,
            new java.lang.String[] {
                "AccountId",
                "CvConfigId",
                "DataCollectionInfo",
                "VerificationTaskId",
                "DataCollectionWorkerId",
            });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
