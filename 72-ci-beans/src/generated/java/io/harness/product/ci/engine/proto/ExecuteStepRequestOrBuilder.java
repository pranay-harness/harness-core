// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/engine/proto/execution.proto

package io.harness.product.ci.engine.proto;

@javax.annotation.Generated(value = "protoc", comments = "annotations:ExecuteStepRequestOrBuilder.java.pb.meta")
public interface ExecuteStepRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.product.ci.engine.proto.ExecuteStepRequest)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.io.harness.product.ci.engine.proto.UnitStep step = 1[json_name = "step"];</code>
   * @return Whether the step field is set.
   */
  boolean hasStep();
  /**
   * <code>.io.harness.product.ci.engine.proto.UnitStep step = 1[json_name = "step"];</code>
   * @return The step.
   */
  io.harness.product.ci.engine.proto.UnitStep getStep();
  /**
   * <code>.io.harness.product.ci.engine.proto.UnitStep step = 1[json_name = "step"];</code>
   */
  io.harness.product.ci.engine.proto.UnitStepOrBuilder getStepOrBuilder();
}
