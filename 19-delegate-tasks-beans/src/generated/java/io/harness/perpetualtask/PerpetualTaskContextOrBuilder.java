// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/perpetual_task.proto

package io.harness.perpetualtask;

@javax.annotation.Generated(value = "protoc", comments = "annotations:PerpetualTaskContextOrBuilder.java.pb.meta")
public interface PerpetualTaskContextOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.PerpetualTaskContext)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskParams task_params = 1;</code>
   */
  boolean hasTaskParams();
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskParams task_params = 1;</code>
   */
  io.harness.perpetualtask.PerpetualTaskParams getTaskParams();
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskParams task_params = 1;</code>
   */
  io.harness.perpetualtask.PerpetualTaskParamsOrBuilder getTaskParamsOrBuilder();

  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskSchedule task_schedule = 2;</code>
   */
  boolean hasTaskSchedule();
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskSchedule task_schedule = 2;</code>
   */
  io.harness.perpetualtask.PerpetualTaskSchedule getTaskSchedule();
  /**
   * <code>.io.harness.perpetualtask.PerpetualTaskSchedule task_schedule = 2;</code>
   */
  io.harness.perpetualtask.PerpetualTaskScheduleOrBuilder getTaskScheduleOrBuilder();

  /**
   * <code>.google.protobuf.Timestamp heartbeat_timestamp = 3;</code>
   */
  boolean hasHeartbeatTimestamp();
  /**
   * <code>.google.protobuf.Timestamp heartbeat_timestamp = 3;</code>
   */
  com.google.protobuf.Timestamp getHeartbeatTimestamp();
  /**
   * <code>.google.protobuf.Timestamp heartbeat_timestamp = 3;</code>
   */
  com.google.protobuf.TimestampOrBuilder getHeartbeatTimestampOrBuilder();
}
