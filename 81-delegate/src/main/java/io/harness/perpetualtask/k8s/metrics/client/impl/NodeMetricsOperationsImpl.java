package io.harness.perpetualtask.k8s.metrics.client.impl;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceOperationsImpl;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.model.node.DoneableNodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import okhttp3.OkHttpClient;

class NodeMetricsOperationsImpl
    extends CustomResourceOperationsImpl<NodeMetrics, NodeMetricsList, DoneableNodeMetrics> {
  NodeMetricsOperationsImpl(OkHttpClient client, Config config) {
    super(client, config, K8sMetricsClient.METRICS_API_GROUP, K8sMetricsClient.METRICS_API_VERSION, "nodes", null, null,
        true, null, null, false, NodeMetrics.class, NodeMetricsList.class, DoneableNodeMetrics.class);
  }

  @Override
  public boolean isResourceNamespaced() {
    return false;
  }
}
