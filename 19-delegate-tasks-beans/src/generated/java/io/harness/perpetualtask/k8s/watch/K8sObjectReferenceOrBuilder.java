// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/k8s/watch/k8s_messages.proto

package io.harness.perpetualtask.k8s.watch;

@javax.annotation.Generated(value = "protoc", comments = "annotations:K8sObjectReferenceOrBuilder.java.pb.meta")
public interface K8sObjectReferenceOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.k8s.watch.K8sObjectReference)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string kind = 1;</code>
   */
  java.lang.String getKind();
  /**
   * <code>string kind = 1;</code>
   */
  com.google.protobuf.ByteString getKindBytes();

  /**
   * <code>string name = 2;</code>
   */
  java.lang.String getName();
  /**
   * <code>string name = 2;</code>
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   * <code>string namespace = 3;</code>
   */
  java.lang.String getNamespace();
  /**
   * <code>string namespace = 3;</code>
   */
  com.google.protobuf.ByteString getNamespaceBytes();

  /**
   * <code>string uid = 4;</code>
   */
  java.lang.String getUid();
  /**
   * <code>string uid = 4;</code>
   */
  com.google.protobuf.ByteString getUidBytes();

  /**
   * <pre>
   * optional
   * </pre>
   *
   * <code>string resource_version = 5;</code>
   */
  java.lang.String getResourceVersion();
  /**
   * <pre>
   * optional
   * </pre>
   *
   * <code>string resource_version = 5;</code>
   */
  com.google.protobuf.ByteString getResourceVersionBytes();
}
