// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: task.proto

package io.harness.delegate.task;

@javax.annotation.Generated(value = "protoc", comments = "annotations:SetupAbstractionsOrBuilder.java.pb.meta")
public interface SetupAbstractionsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.delegate.task.SetupAbstractions)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>map&lt;string, string&gt; values = 1;</code>
   */
  int getValuesCount();
  /**
   * <code>map&lt;string, string&gt; values = 1;</code>
   */
  boolean containsValues(java.lang.String key);
  /**
   * Use {@link #getValuesMap()} instead.
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.String> getValues();
  /**
   * <code>map&lt;string, string&gt; values = 1;</code>
   */
  java.util.Map<java.lang.String, java.lang.String> getValuesMap();
  /**
   * <code>map&lt;string, string&gt; values = 1;</code>
   */

  java.lang.String getValuesOrDefault(java.lang.String key, java.lang.String defaultValue);
  /**
   * <code>map&lt;string, string&gt; values = 1;</code>
   */

  java.lang.String getValuesOrThrow(java.lang.String key);
}
