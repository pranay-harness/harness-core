package io.harness.perpetualtask.k8s.metrics.client.model.node;

import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResource;

import com.google.gson.annotations.SerializedName;
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
  @SerializedName("timestamp") private String timestamp;
  @SerializedName("window") private String window;
  @SerializedName("usage") private Usage usage;

  @Builder
  public NodeMetrics(String name, String timestamp, String window, Usage usage) {
    this.getMetadata().setName(name);
    this.timestamp = timestamp;
    this.window = window;
    this.usage = usage;
  }
}
