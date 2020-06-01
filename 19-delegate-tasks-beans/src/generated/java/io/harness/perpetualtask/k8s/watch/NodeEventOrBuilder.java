// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/k8s/watch/k8s_messages.proto

package io.harness.perpetualtask.k8s.watch;

@javax.annotation.Generated(value = "protoc", comments = "annotations:NodeEventOrBuilder.java.pb.meta")
public interface NodeEventOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.k8s.watch.NodeEvent)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string cloud_provider_id = 2[json_name = "cloudProviderId"];</code>
   * @return The cloudProviderId.
   */
  java.lang.String getCloudProviderId();
  /**
   * <code>string cloud_provider_id = 2[json_name = "cloudProviderId"];</code>
   * @return The bytes for cloudProviderId.
   */
  com.google.protobuf.ByteString getCloudProviderIdBytes();

  /**
   * <code>string node_uid = 3[json_name = "nodeUid"];</code>
   * @return The nodeUid.
   */
  java.lang.String getNodeUid();
  /**
   * <code>string node_uid = 3[json_name = "nodeUid"];</code>
   * @return The bytes for nodeUid.
   */
  com.google.protobuf.ByteString getNodeUidBytes();

  /**
   * <code>.io.harness.perpetualtask.k8s.watch.NodeEvent.EventType type = 4[json_name = "type"];</code>
   * @return The enum numeric value on the wire for type.
   */
  int getTypeValue();
  /**
   * <code>.io.harness.perpetualtask.k8s.watch.NodeEvent.EventType type = 4[json_name = "type"];</code>
   * @return The type.
   */
  io.harness.perpetualtask.k8s.watch.NodeEvent.EventType getType();

  /**
   * <code>.google.protobuf.Timestamp timestamp = 5[json_name = "timestamp"];</code>
   * @return Whether the timestamp field is set.
   */
  boolean hasTimestamp();
  /**
   * <code>.google.protobuf.Timestamp timestamp = 5[json_name = "timestamp"];</code>
   * @return The timestamp.
   */
  com.google.protobuf.Timestamp getTimestamp();
  /**
   * <code>.google.protobuf.Timestamp timestamp = 5[json_name = "timestamp"];</code>
   */
  com.google.protobuf.TimestampOrBuilder getTimestampOrBuilder();

  /**
   * <code>string node_name = 6[json_name = "nodeName"];</code>
   * @return The nodeName.
   */
  java.lang.String getNodeName();
  /**
   * <code>string node_name = 6[json_name = "nodeName"];</code>
   * @return The bytes for nodeName.
   */
  com.google.protobuf.ByteString getNodeNameBytes();

  /**
   * <code>string cluster_id = 7[json_name = "clusterId"];</code>
   * @return The clusterId.
   */
  java.lang.String getClusterId();
  /**
   * <code>string cluster_id = 7[json_name = "clusterId"];</code>
   * @return The bytes for clusterId.
   */
  com.google.protobuf.ByteString getClusterIdBytes();

  /**
   * <code>string kube_system_uid = 8[json_name = "kubeSystemUid"];</code>
   * @return The kubeSystemUid.
   */
  java.lang.String getKubeSystemUid();
  /**
   * <code>string kube_system_uid = 8[json_name = "kubeSystemUid"];</code>
   * @return The bytes for kubeSystemUid.
   */
  com.google.protobuf.ByteString getKubeSystemUidBytes();
}
