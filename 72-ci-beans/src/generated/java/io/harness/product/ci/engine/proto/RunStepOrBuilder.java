// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/engine/proto/execution.proto

package io.harness.product.ci.engine.proto;

@javax.annotation.Generated(value = "protoc", comments = "annotations:RunStepOrBuilder.java.pb.meta")
public interface RunStepOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.product.ci.engine.proto.RunStep)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>repeated string commands = 1[json_name = "commands"];</code>
   * @return A list containing the commands.
   */
  java.util.List<java.lang.String> getCommandsList();
  /**
   * <code>repeated string commands = 1[json_name = "commands"];</code>
   * @return The count of commands.
   */
  int getCommandsCount();
  /**
   * <code>repeated string commands = 1[json_name = "commands"];</code>
   * @param index The index of the element to return.
   * @return The commands at the given index.
   */
  java.lang.String getCommands(int index);
  /**
   * <code>repeated string commands = 1[json_name = "commands"];</code>
   * @param index The index of the value to return.
   * @return The bytes of the commands at the given index.
   */
  com.google.protobuf.ByteString getCommandsBytes(int index);

  /**
   * <code>.io.harness.product.ci.engine.proto.StepContext context = 2[json_name = "context"];</code>
   * @return Whether the context field is set.
   */
  boolean hasContext();
  /**
   * <code>.io.harness.product.ci.engine.proto.StepContext context = 2[json_name = "context"];</code>
   * @return The context.
   */
  io.harness.product.ci.engine.proto.StepContext getContext();
  /**
   * <code>.io.harness.product.ci.engine.proto.StepContext context = 2[json_name = "context"];</code>
   */
  io.harness.product.ci.engine.proto.StepContextOrBuilder getContextOrBuilder();
}
