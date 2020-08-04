package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResourceList;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PodMetricsList extends CustomResourceList<PodMetrics> {
  @Builder
  public PodMetricsList(@Singular List<PodMetrics> items) {
    this.setKind("PodMetricsList");
    this.setItems(items);
  }
}
