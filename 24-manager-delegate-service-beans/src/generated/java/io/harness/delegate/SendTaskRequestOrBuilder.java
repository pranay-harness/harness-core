// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/ng_delegate_task.proto

package io.harness.delegate;

@javax.annotation.Generated(value = "protoc", comments = "annotations:SendTaskRequestOrBuilder.java.pb.meta")
public interface SendTaskRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.delegate.SendTaskRequest)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.io.harness.delegate.NgAccountId account_id = 2[json_name = "accountId"];</code>
   * @return Whether the accountId field is set.
   */
  boolean hasAccountId();
  /**
   * <code>.io.harness.delegate.NgAccountId account_id = 2[json_name = "accountId"];</code>
   * @return The accountId.
   */
  io.harness.delegate.NgAccountId getAccountId();
  /**
   * <code>.io.harness.delegate.NgAccountId account_id = 2[json_name = "accountId"];</code>
   */
  io.harness.delegate.NgAccountIdOrBuilder getAccountIdOrBuilder();

  /**
   * <code>.io.harness.delegate.NgTaskSetupAbstractions setup_abstractions = 3[json_name = "setupAbstractions"];</code>
   * @return Whether the setupAbstractions field is set.
   */
  boolean hasSetupAbstractions();
  /**
   * <code>.io.harness.delegate.NgTaskSetupAbstractions setup_abstractions = 3[json_name = "setupAbstractions"];</code>
   * @return The setupAbstractions.
   */
  io.harness.delegate.NgTaskSetupAbstractions getSetupAbstractions();
  /**
   * <code>.io.harness.delegate.NgTaskSetupAbstractions setup_abstractions = 3[json_name = "setupAbstractions"];</code>
   */
  io.harness.delegate.NgTaskSetupAbstractionsOrBuilder getSetupAbstractionsOrBuilder();

  /**
   * <code>.io.harness.delegate.NgTaskDetails details = 4[json_name = "details"];</code>
   * @return Whether the details field is set.
   */
  boolean hasDetails();
  /**
   * <code>.io.harness.delegate.NgTaskDetails details = 4[json_name = "details"];</code>
   * @return The details.
   */
  io.harness.delegate.NgTaskDetails getDetails();
  /**
   * <code>.io.harness.delegate.NgTaskDetails details = 4[json_name = "details"];</code>
   */
  io.harness.delegate.NgTaskDetailsOrBuilder getDetailsOrBuilder();
}
