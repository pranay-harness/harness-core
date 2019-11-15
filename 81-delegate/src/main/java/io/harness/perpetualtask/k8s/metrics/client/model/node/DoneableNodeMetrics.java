package io.harness.perpetualtask.k8s.metrics.client.model.node;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableNodeMetrics extends CustomResourceDoneable<NodeMetrics> {
  public DoneableNodeMetrics(NodeMetrics resource, Function<NodeMetrics, NodeMetrics> function) {
    super(resource, function);
  }
}
