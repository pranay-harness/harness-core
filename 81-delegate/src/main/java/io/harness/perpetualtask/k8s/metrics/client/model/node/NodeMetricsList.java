package io.harness.perpetualtask.k8s.metrics.client.model.node;

import io.fabric8.kubernetes.client.CustomResourceList;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@NoArgsConstructor
public class NodeMetricsList extends CustomResourceList<NodeMetrics> {
  @Builder
  public NodeMetricsList(@Singular List<NodeMetrics> items) {
    this.setApiVersion("v1");
    this.setKind("List");
    this.setItems(items);
  }
}
