// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: product/ci/engine/proto/execution.proto

package io.harness.product.ci.engine.proto;

@javax.annotation.Generated(value = "protoc", comments = "annotations:ExecuteStepResponseOrBuilder.java.pb.meta")
public interface ExecuteStepResponseOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.product.ci.engine.proto.ExecuteStepResponse)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>map&lt;string, string&gt; output = 1[json_name = "output"];</code>
   */
  int getOutputCount();
  /**
   * <code>map&lt;string, string&gt; output = 1[json_name = "output"];</code>
   */
  boolean containsOutput(java.lang.String key);
  /**
   * Use {@link #getOutputMap()} instead.
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.String> getOutput();
  /**
   * <code>map&lt;string, string&gt; output = 1[json_name = "output"];</code>
   */
  java.util.Map<java.lang.String, java.lang.String> getOutputMap();
  /**
   * <code>map&lt;string, string&gt; output = 1[json_name = "output"];</code>
   */

  java.lang.String getOutputOrDefault(java.lang.String key, java.lang.String defaultValue);
  /**
   * <code>map&lt;string, string&gt; output = 1[json_name = "output"];</code>
   */

  java.lang.String getOutputOrThrow(java.lang.String key);
}
