// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/delegate.proto

package io.harness.delegate;

@javax.annotation.Generated(value = "protoc", comments = "annotations:Delegate.java.pb.meta")
public final class Delegate {
  private Delegate() {}
  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor internal_static_io_harness_delegate_DelegateId_descriptor;
  static final com.google.protobuf.GeneratedMessageV3
      .FieldAccessorTable internal_static_io_harness_delegate_DelegateId_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    java.lang.String[] descriptorData = {"\n\"io/harness/delegate/delegate.proto\022\023io"
        + ".harness.delegate\"\030\n\nDelegateId\022\n\n\002id\030\001 "
        + "\001(\tB\002P\001b\006proto3"};
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
        descriptorData, new com.google.protobuf.Descriptors.FileDescriptor[] {}, assigner);
    internal_static_io_harness_delegate_DelegateId_descriptor = getDescriptor().getMessageTypes().get(0);
    internal_static_io_harness_delegate_DelegateId_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_io_harness_delegate_DelegateId_descriptor,
            new java.lang.String[] {
                "Id",
            });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
