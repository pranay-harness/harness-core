package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import io.fabric8.kubernetes.client.CustomResourceList;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@NoArgsConstructor
public class PodMetricsList extends CustomResourceList<PodMetrics> {
  @Builder
  public PodMetricsList(@Singular List<PodMetrics> items) {
    this.setApiVersion("v1");
    this.setKind("List");
    this.setItems(items);
  }
}
