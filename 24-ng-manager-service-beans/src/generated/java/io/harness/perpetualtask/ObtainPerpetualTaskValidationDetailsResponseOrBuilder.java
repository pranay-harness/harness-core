// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/ng_perpetualtask_service_client.proto

package io.harness.perpetualtask;

@javax.annotation.
Generated(value = "protoc", comments = "annotations:ObtainPerpetualTaskValidationDetailsResponseOrBuilder.java.pb.meta")
public interface ObtainPerpetualTaskValidationDetailsResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.ObtainPerpetualTaskValidationDetailsResponse)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.io.harness.delegate.NgTaskSetupAbstractions setup_abstractions = 1[json_name = "setupAbstractions"];</code>
   * @return Whether the setupAbstractions field is set.
   */
  boolean hasSetupAbstractions();
  /**
   * <code>.io.harness.delegate.NgTaskSetupAbstractions setup_abstractions = 1[json_name = "setupAbstractions"];</code>
   * @return The setupAbstractions.
   */
  io.harness.delegate.NgTaskSetupAbstractions getSetupAbstractions();
  /**
   * <code>.io.harness.delegate.NgTaskSetupAbstractions setup_abstractions = 1[json_name = "setupAbstractions"];</code>
   */
  io.harness.delegate.NgTaskSetupAbstractionsOrBuilder getSetupAbstractionsOrBuilder();

  /**
   * <code>.io.harness.delegate.NgTaskDetails details = 2[json_name = "details"];</code>
   * @return Whether the details field is set.
   */
  boolean hasDetails();
  /**
   * <code>.io.harness.delegate.NgTaskDetails details = 2[json_name = "details"];</code>
   * @return The details.
   */
  io.harness.delegate.NgTaskDetails getDetails();
  /**
   * <code>.io.harness.delegate.NgTaskDetails details = 2[json_name = "details"];</code>
   */
  io.harness.delegate.NgTaskDetailsOrBuilder getDetailsOrBuilder();
}
