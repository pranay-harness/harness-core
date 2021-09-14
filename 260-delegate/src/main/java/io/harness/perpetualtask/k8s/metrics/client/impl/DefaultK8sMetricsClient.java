/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.k8s.metrics.client.impl;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.k8s.apiclient.GenericKubernetesApi;
import io.harness.k8s.apiclient.NodeStatsClient;
import io.harness.k8s.model.statssummary.PodStatsList;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.node.NodeMetricsList;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class DefaultK8sMetricsClient extends CoreV1Api implements K8sMetricsClient {
  public DefaultK8sMetricsClient(ApiClient apiClient) {
    super(apiClient);
  }

  @Override
  public GenericKubernetesApi<NodeMetrics, NodeMetricsList> nodeMetrics() {
    return new GenericKubernetesApi<>(
        NodeMetrics.class, NodeMetricsList.class, METRICS_API_GROUP, METRICS_API_VERSION, "nodes", this.getApiClient());
  }

  @Override
  public GenericKubernetesApi<PodMetrics, PodMetricsList> podMetrics() {
    return new GenericKubernetesApi<>(
        PodMetrics.class, PodMetricsList.class, METRICS_API_GROUP, METRICS_API_VERSION, "pods", this.getApiClient());
  }

  @Override
  public NodeStatsClient<PodStatsList> podStats() {
    return new NodeStatsClient<>(PodStatsList.class, "", "v1", this.getApiClient());
  }
}
