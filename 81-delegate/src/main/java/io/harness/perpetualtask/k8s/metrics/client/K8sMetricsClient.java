package io.harness.perpetualtask.k8s.metrics.client;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.harness.perpetualtask.k8s.metrics.client.model.node.DoneableNodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.DoneablePodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;

public interface K8sMetricsClient {
  String METRICS_API_GROUP = "metrics.k8s.io";
  String METRICS_API_VERSION = "v1beta1";

  NonNamespaceOperation<NodeMetrics, NodeMetricsList, DoneableNodeMetrics, Resource<NodeMetrics, DoneableNodeMetrics>>
  nodeMetrics();

  MixedOperation<PodMetrics, PodMetricsList, DoneablePodMetrics, Resource<PodMetrics, DoneablePodMetrics>> podMetrics();
}
