package io.harness.perpetualtask.k8s.metrics.client.impl;

import io.fabric8.kubernetes.client.BaseClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.model.node.DoneableNodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.DoneablePodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;
import okhttp3.OkHttpClient;

public class DefaultK8sMetricsClient extends BaseClient implements K8sMetricsClient {
  public DefaultK8sMetricsClient() {
    // default
  }

  public DefaultK8sMetricsClient(Config config) {
    super(config);
  }

  public DefaultK8sMetricsClient(OkHttpClient httpClient, Config config) {
    super(httpClient, config);
  }

  @Override
  public NonNamespaceOperation<NodeMetrics, NodeMetricsList, DoneableNodeMetrics,
      Resource<NodeMetrics, DoneableNodeMetrics>>
  nodeMetrics() {
    return new NodeMetricsOperationsImpl(getHttpClient(), getConfiguration());
  }

  @Override
  public MixedOperation<PodMetrics, PodMetricsList, DoneablePodMetrics, Resource<PodMetrics, DoneablePodMetrics>>
  podMetrics() {
    return new PodMetricsOperationsImpl(getHttpClient(), getConfiguration(), getNamespace());
  }
}
