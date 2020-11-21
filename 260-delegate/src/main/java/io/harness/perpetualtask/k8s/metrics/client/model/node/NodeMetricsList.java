package io.harness.perpetualtask.k8s.metrics.client.model.node;

import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResourceList;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NodeMetricsList extends CustomResourceList<NodeMetrics> {
  @Builder
  public NodeMetricsList(@Singular List<NodeMetrics> items) {
    this.setKind("NodeMetricsList");
    this.setItems(items);
  }
}
