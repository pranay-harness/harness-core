// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/event/event_publisher.proto

package io.harness.event;

@javax.annotation.Generated(value = "protoc", comments = "annotations:PublishRequestOrBuilder.java.pb.meta")
public interface PublishRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.event.PublishRequest)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>repeated .io.harness.event.PublishMessage messages = 1;</code>
   */
  java.util.List<io.harness.event.PublishMessage> getMessagesList();
  /**
   * <code>repeated .io.harness.event.PublishMessage messages = 1;</code>
   */
  io.harness.event.PublishMessage getMessages(int index);
  /**
   * <code>repeated .io.harness.event.PublishMessage messages = 1;</code>
   */
  int getMessagesCount();
  /**
   * <code>repeated .io.harness.event.PublishMessage messages = 1;</code>
   */
  java.util.List<? extends io.harness.event.PublishMessageOrBuilder> getMessagesOrBuilderList();
  /**
   * <code>repeated .io.harness.event.PublishMessage messages = 1;</code>
   */
  io.harness.event.PublishMessageOrBuilder getMessagesOrBuilder(int index);
}
