package io.harness.perpetualtask.k8s.metrics.client.model.node;

import io.fabric8.kubernetes.client.CustomResource;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NodeMetrics extends CustomResource {
  private String timestamp;
  private String window;
  private Usage usage;

  @Builder
  public NodeMetrics(String name, String timestamp, String window, Usage usage) {
    this.getMetadata().setName(name);
    this.timestamp = timestamp;
    this.window = window;
    this.usage = usage;
  }
}
