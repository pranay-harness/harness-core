// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: delegate_service.proto

package io.harness.delegate.progress;

@javax.annotation.Generated(value = "protoc", comments = "annotations:SubmitTaskRequestOrBuilder.java.pb.meta")
public interface SubmitTaskRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.delegate.progress.SubmitTaskRequest)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.io.harness.delegate.task.SetupAbstractions setup_abstractions = 1;</code>
   */
  boolean hasSetupAbstractions();
  /**
   * <code>.io.harness.delegate.task.SetupAbstractions setup_abstractions = 1;</code>
   */
  io.harness.delegate.task.SetupAbstractions getSetupAbstractions();
  /**
   * <code>.io.harness.delegate.task.SetupAbstractions setup_abstractions = 1;</code>
   */
  io.harness.delegate.task.SetupAbstractionsOrBuilder getSetupAbstractionsOrBuilder();

  /**
   * <code>.io.harness.delegate.task.Details details = 2;</code>
   */
  boolean hasDetails();
  /**
   * <code>.io.harness.delegate.task.Details details = 2;</code>
   */
  io.harness.delegate.task.Details getDetails();
  /**
   * <code>.io.harness.delegate.task.Details details = 2;</code>
   */
  io.harness.delegate.task.DetailsOrBuilder getDetailsOrBuilder();
}
