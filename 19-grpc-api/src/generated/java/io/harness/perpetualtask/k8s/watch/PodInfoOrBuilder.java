// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/perpetualtask/k8s.watch/k8s_messages.proto

package io.harness.perpetualtask.k8s.watch;

@javax.annotation.Generated(value = "protoc", comments = "annotations:PodInfoOrBuilder.java.pb.meta")
public interface PodInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:io.harness.perpetualtask.k8s.watch.PodInfo)
    com.google.protobuf.MessageOrBuilder {
  /**
   * <code>string cloud_provider_id = 2;</code>
   */
  java.lang.String getCloudProviderId();
  /**
   * <code>string cloud_provider_id = 2;</code>
   */
  com.google.protobuf.ByteString getCloudProviderIdBytes();

  /**
   * <code>string pod_uid = 3;</code>
   */
  java.lang.String getPodUid();
  /**
   * <code>string pod_uid = 3;</code>
   */
  com.google.protobuf.ByteString getPodUidBytes();

  /**
   * <code>string pod_name = 4;</code>
   */
  java.lang.String getPodName();
  /**
   * <code>string pod_name = 4;</code>
   */
  com.google.protobuf.ByteString getPodNameBytes();

  /**
   * <code>string namespace = 5;</code>
   */
  java.lang.String getNamespace();
  /**
   * <code>string namespace = 5;</code>
   */
  com.google.protobuf.ByteString getNamespaceBytes();

  /**
   * <code>string node_name = 6;</code>
   */
  java.lang.String getNodeName();
  /**
   * <code>string node_name = 6;</code>
   */
  com.google.protobuf.ByteString getNodeNameBytes();

  /**
   * <code>.io.harness.perpetualtask.k8s.watch.Resource total_resource = 7;</code>
   */
  boolean hasTotalResource();
  /**
   * <code>.io.harness.perpetualtask.k8s.watch.Resource total_resource = 7;</code>
   */
  io.harness.perpetualtask.k8s.watch.Resource getTotalResource();
  /**
   * <code>.io.harness.perpetualtask.k8s.watch.Resource total_resource = 7;</code>
   */
  io.harness.perpetualtask.k8s.watch.ResourceOrBuilder getTotalResourceOrBuilder();

  /**
   * <code>.google.protobuf.Timestamp creation_timestamp = 8;</code>
   */
  boolean hasCreationTimestamp();
  /**
   * <code>.google.protobuf.Timestamp creation_timestamp = 8;</code>
   */
  com.google.protobuf.Timestamp getCreationTimestamp();
  /**
   * <code>.google.protobuf.Timestamp creation_timestamp = 8;</code>
   */
  com.google.protobuf.TimestampOrBuilder getCreationTimestampOrBuilder();

  /**
   * <pre>
   * label without value is invalid
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 9;</code>
   */
  int getLabelsCount();
  /**
   * <pre>
   * label without value is invalid
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 9;</code>
   */
  boolean containsLabels(java.lang.String key);
  /**
   * Use {@link #getLabelsMap()} instead.
   */
  @java.lang.Deprecated java.util.Map<java.lang.String, java.lang.String> getLabels();
  /**
   * <pre>
   * label without value is invalid
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 9;</code>
   */
  java.util.Map<java.lang.String, java.lang.String> getLabelsMap();
  /**
   * <pre>
   * label without value is invalid
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 9;</code>
   */

  java.lang.String getLabelsOrDefault(java.lang.String key, java.lang.String defaultValue);
  /**
   * <pre>
   * label without value is invalid
   * </pre>
   *
   * <code>map&lt;string, string&gt; labels = 9;</code>
   */

  java.lang.String getLabelsOrThrow(java.lang.String key);

  /**
   * <code>.io.harness.perpetualtask.k8s.watch.Owner top_level_owner = 11;</code>
   */
  boolean hasTopLevelOwner();
  /**
   * <code>.io.harness.perpetualtask.k8s.watch.Owner top_level_owner = 11;</code>
   */
  io.harness.perpetualtask.k8s.watch.Owner getTopLevelOwner();
  /**
   * <code>.io.harness.perpetualtask.k8s.watch.Owner top_level_owner = 11;</code>
   */
  io.harness.perpetualtask.k8s.watch.OwnerOrBuilder getTopLevelOwnerOrBuilder();

  /**
   * <code>repeated .io.harness.perpetualtask.k8s.watch.Container containers = 12;</code>
   */
  java.util.List<io.harness.perpetualtask.k8s.watch.Container> getContainersList();
  /**
   * <code>repeated .io.harness.perpetualtask.k8s.watch.Container containers = 12;</code>
   */
  io.harness.perpetualtask.k8s.watch.Container getContainers(int index);
  /**
   * <code>repeated .io.harness.perpetualtask.k8s.watch.Container containers = 12;</code>
   */
  int getContainersCount();
  /**
   * <code>repeated .io.harness.perpetualtask.k8s.watch.Container containers = 12;</code>
   */
  java.util.List<? extends io.harness.perpetualtask.k8s.watch.ContainerOrBuilder> getContainersOrBuilderList();
  /**
   * <code>repeated .io.harness.perpetualtask.k8s.watch.Container containers = 12;</code>
   */
  io.harness.perpetualtask.k8s.watch.ContainerOrBuilder getContainersOrBuilder(int index);

  /**
   * <code>string cluster_id = 13;</code>
   */
  java.lang.String getClusterId();
  /**
   * <code>string cluster_id = 13;</code>
   */
  com.google.protobuf.ByteString getClusterIdBytes();

  /**
   * <code>string cluster_name = 14;</code>
   */
  java.lang.String getClusterName();
  /**
   * <code>string cluster_name = 14;</code>
   */
  com.google.protobuf.ByteString getClusterNameBytes();

  /**
   * <code>string kube_system_uid = 15;</code>
   */
  java.lang.String getKubeSystemUid();
  /**
   * <code>string kube_system_uid = 15;</code>
   */
  com.google.protobuf.ByteString getKubeSystemUidBytes();
}
