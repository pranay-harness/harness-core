// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/ng_delegate_task.proto

package io.harness.delegate;

@javax.annotation.Generated(value = "protoc", comments = "annotations:SendTaskResponseOrBuilder.java.pb.meta")
public interface SendTaskResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.delegate.SendTaskResponse)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.io.harness.delegate.NgTaskId task_id = 1[json_name = "taskId"];</code>
   * @return Whether the taskId field is set.
   */
  boolean hasTaskId();
  /**
   * <code>.io.harness.delegate.NgTaskId task_id = 1[json_name = "taskId"];</code>
   * @return The taskId.
   */
  io.harness.delegate.NgTaskId getTaskId();
  /**
   * <code>.io.harness.delegate.NgTaskId task_id = 1[json_name = "taskId"];</code>
   */
  io.harness.delegate.NgTaskIdOrBuilder getTaskIdOrBuilder();

  /**
   * <code>bytes response_data = 2[json_name = "responseData"];</code>
   * @return The responseData.
   */
  com.google.protobuf.ByteString getResponseData();
}
