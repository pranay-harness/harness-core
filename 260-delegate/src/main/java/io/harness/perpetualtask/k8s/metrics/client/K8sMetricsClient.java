package io.harness.perpetualtask.k8s.metrics.client;

import io.harness.k8s.apiclient.GenericKubernetesApi;
import io.harness.k8s.apiclient.NodeStatsClient;
import io.harness.k8s.model.statssummary.PodStatsList;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;

public interface K8sMetricsClient {
  String METRICS_API_GROUP = "metrics.k8s.io";
  String METRICS_API_VERSION = "v1beta1";

  GenericKubernetesApi<NodeMetrics, NodeMetricsList> nodeMetrics();

  GenericKubernetesApi<PodMetrics, PodMetricsList> podMetrics();

  NodeStatsClient<PodStatsList> podStats();
}
