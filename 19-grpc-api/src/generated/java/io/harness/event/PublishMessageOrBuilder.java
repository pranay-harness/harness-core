// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/event/event_publisher.proto

package io.harness.event;

@javax.annotation.Generated(value = "protoc", comments = "annotations:PublishMessageOrBuilder.java.pb.meta")
public interface PublishMessageOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.event.PublishMessage)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>.google.protobuf.Any payload = 1;</code>
   */
  boolean hasPayload();
  /**
   * <code>.google.protobuf.Any payload = 1;</code>
   */
  com.google.protobuf.Any getPayload();
  /**
   * <code>.google.protobuf.Any payload = 1;</code>
   */
  com.google.protobuf.AnyOrBuilder getPayloadOrBuilder();

  /**
   * <code>map&lt;string, string&gt; attributes = 2;</code>
   */
  int getAttributesCount();
  /**
   * <code>map&lt;string, string&gt; attributes = 2;</code>
   */
  boolean containsAttributes(java.lang.String key);
  /**
   * Use {@link #getAttributesMap()} instead.
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.String> getAttributes();
  /**
   * <code>map&lt;string, string&gt; attributes = 2;</code>
   */
  java.util.Map<java.lang.String, java.lang.String> getAttributesMap();
  /**
   * <code>map&lt;string, string&gt; attributes = 2;</code>
   */

  java.lang.String getAttributesOrDefault(java.lang.String key, java.lang.String defaultValue);
  /**
   * <code>map&lt;string, string&gt; attributes = 2;</code>
   */

  java.lang.String getAttributesOrThrow(java.lang.String key);

  /**
   * <code>.google.protobuf.Timestamp occurred_at = 3;</code>
   */
  boolean hasOccurredAt();
  /**
   * <code>.google.protobuf.Timestamp occurred_at = 3;</code>
   */
  com.google.protobuf.Timestamp getOccurredAt();
  /**
   * <code>.google.protobuf.Timestamp occurred_at = 3;</code>
   */
  com.google.protobuf.TimestampOrBuilder getOccurredAtOrBuilder();
}
