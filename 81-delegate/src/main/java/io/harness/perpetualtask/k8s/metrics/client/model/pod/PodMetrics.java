package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import static io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient.METRICS_API_GROUP;
import static io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient.METRICS_API_VERSION;

import io.fabric8.kubernetes.client.CustomResource;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PodMetrics extends CustomResource {
  private String timestamp;
  private String window;
  private List<Container> containers;

  @Builder
  public PodMetrics(
      String name, String namespace, String timestamp, String window, @Singular List<Container> containers) {
    this.getMetadata().setName(name);
    this.getMetadata().setNamespace(namespace);
    this.setKind("PodMetrics");
    this.setApiVersion(METRICS_API_GROUP + "/" + METRICS_API_VERSION);
    this.timestamp = timestamp;
    this.window = window;
    this.containers = containers;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Container implements Serializable {
    String name;
    Usage usage;
  }
}
