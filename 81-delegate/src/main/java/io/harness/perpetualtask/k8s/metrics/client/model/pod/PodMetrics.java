package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import io.fabric8.kubernetes.client.CustomResource;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PodMetrics extends CustomResource {
  private String timestamp;
  private String window;
  private List<Container> containers;

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Container implements Serializable {
    String name;
    Usage usage;
  }
}
