// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task.proto

package io.harness.perpetualtask;

@javax.annotation.Generated(value = "protoc", comments = "annotations:PerpetualTaskAssignDetailsOrBuilder.java.pb.meta")
public interface PerpetualTaskAssignDetailsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.PerpetualTaskAssignDetails)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskId task_id = 1;</code>
   */
  boolean hasTaskId();
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskId task_id = 1;</code>
   */
  io.harness.perpetualtask.PerpetualTaskId getTaskId();
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskId task_id = 1;</code>
   */
  io.harness.perpetualtask.PerpetualTaskIdOrBuilder getTaskIdOrBuilder();

  /**
   * <code>.google.protobuf.Timestamp last_context_updated = 2;</code>
   */
  boolean hasLastContextUpdated();
  /**
   * <code>.google.protobuf.Timestamp last_context_updated = 2;</code>
   */
  com.google.protobuf.Timestamp getLastContextUpdated();
  /**
   * <code>.google.protobuf.Timestamp last_context_updated = 2;</code>
   */
  com.google.protobuf.TimestampOrBuilder getLastContextUpdatedOrBuilder();
}
