// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/addon/proto/addon.proto

package io.harness.product.ci.addon.proto;

@javax.annotation.Generated(value = "protoc", comments = "annotations:TaskProgressResponseOrBuilder.java.pb.meta")
public interface TaskProgressResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.product.ci.addon.proto.TaskProgressResponse)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
   * @return The enum numeric value on the wire for currentStage.
   */
  int getCurrentStageValue();
  /**
   * <code>.io.harness.product.ci.addon.proto.TaskStatus current_stage = 1[json_name = "currentStage"];</code>
   * @return The currentStage.
   */
  io.harness.product.ci.addon.proto.TaskStatus getCurrentStage();

  /**
   * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
   * @return Whether the error field is set.
   */
  boolean hasError();
  /**
   * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
   * @return The error.
   */
  io.harness.product.ci.addon.proto.Error getError();
  /**
   * <code>.io.harness.product.ci.addon.proto.Error error = 2[json_name = "error"];</code>
   */
  io.harness.product.ci.addon.proto.ErrorOrBuilder getErrorOrBuilder();
}
