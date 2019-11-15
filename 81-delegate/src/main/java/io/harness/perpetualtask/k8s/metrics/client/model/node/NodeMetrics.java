package io.harness.perpetualtask.k8s.metrics.client.model.node;

import io.fabric8.kubernetes.client.CustomResource;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NodeMetrics extends CustomResource {
  private String timestamp;
  private String window;
  private Usage usage;
}
