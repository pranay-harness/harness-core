// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/k8s/watch/k8s_watch.proto

package io.harness.perpetualtask.k8s.watch;

@javax.annotation.Generated(value = "protoc", comments = "annotations:K8sWatchTaskParamsOrBuilder.java.pb.meta")
public interface K8sWatchTaskParamsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.k8s.watch.K8sWatchTaskParams)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string cloud_provider_id = 1;</code>
   */
  java.lang.String getCloudProviderId();
  /**
   * <code>string cloud_provider_id = 1;</code>
   */
  com.google.protobuf.ByteString getCloudProviderIdBytes();

  /**
   * <code>bytes k8s_cluster_config = 2;</code>
   */
  com.google.protobuf.ByteString getK8SClusterConfig();

  /**
   * <code>string cluster_id = 3;</code>
   */
  java.lang.String getClusterId();
  /**
   * <code>string cluster_id = 3;</code>
   */
  com.google.protobuf.ByteString getClusterIdBytes();

  /**
   * <code>string cluster_name = 4;</code>
   */
  java.lang.String getClusterName();
  /**
   * <code>string cluster_name = 4;</code>
   */
  com.google.protobuf.ByteString getClusterNameBytes();
}
