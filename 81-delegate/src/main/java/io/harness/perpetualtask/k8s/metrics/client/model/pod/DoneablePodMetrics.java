package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneablePodMetrics extends CustomResourceDoneable<PodMetrics> {
  public DoneablePodMetrics(PodMetrics resource, Function<PodMetrics, PodMetrics> function) {
    super(resource, function);
  }
}
