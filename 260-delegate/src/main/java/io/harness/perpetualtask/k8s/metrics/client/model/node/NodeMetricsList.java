package io.harness.perpetualtask.k8s.metrics.client.model.node;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResourceList;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._420_DELEGATE_AGENT)
public class NodeMetricsList extends CustomResourceList<NodeMetrics> {
  @Builder
  public NodeMetricsList(@Singular List<NodeMetrics> items) {
    this.setKind("NodeMetricsList");
    this.setItems(items);
  }
}
