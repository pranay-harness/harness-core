// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/callback/mongodb.proto

package io.harness.callback;

@javax.annotation.Generated(value = "protoc", comments = "annotations:Mongodb.java.pb.meta")
public final class Mongodb {
  private Mongodb() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor internal_static_io_harness_callback_MongoDatabase_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_callback_MongoDatabase_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n!io/harness/callback/mongodb.proto\022\023io."
        + "harness.callback\"e\n\rMongoDatabase\022\036\n\ncon"
        + "nection\030\001 \001(\tR\nconnection\0224\n\026collection_"
        + "name_prefix\030\002 \001(\tR\024collectionNamePrefixB"
        + "\002P\001b\006proto3"};
    descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
        descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {});
    internal_static_io_harness_callback_MongoDatabase_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_callback_MongoDatabase_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_callback_MongoDatabase_descriptor,
            new java.lang.String[] {
                "Connection",
                "CollectionNamePrefix",
            });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
