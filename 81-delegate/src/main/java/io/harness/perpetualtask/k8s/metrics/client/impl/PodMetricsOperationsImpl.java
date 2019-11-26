package io.harness.perpetualtask.k8s.metrics.client.impl;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceOperationsImpl;
import io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.DoneablePodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetrics;
import io.harness.perpetualtask.k8s.metrics.client.model.pod.PodMetricsList;
import okhttp3.OkHttpClient;

class PodMetricsOperationsImpl extends CustomResourceOperationsImpl<PodMetrics, PodMetricsList, DoneablePodMetrics> {
  PodMetricsOperationsImpl(OkHttpClient client, Config config, String namespace) {
    super(client, config, K8sMetricsClient.METRICS_API_GROUP, K8sMetricsClient.METRICS_API_VERSION, "pods", namespace,
        null, true, null, null, false, PodMetrics.class, PodMetricsList.class, DoneablePodMetrics.class);
  }
}
