package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResourceList;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PodMetricsList extends CustomResourceList<PodMetrics> {
  @Builder
  public PodMetricsList(@Singular List<PodMetrics> items) {
    this.setKind("PodMetricsList");
    this.setItems(items);
  }
}
