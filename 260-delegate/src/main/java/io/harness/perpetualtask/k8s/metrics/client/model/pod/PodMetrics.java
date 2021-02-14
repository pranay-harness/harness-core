package io.harness.perpetualtask.k8s.metrics.client.model.pod;

import static io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient.METRICS_API_GROUP;
import static io.harness.perpetualtask.k8s.metrics.client.K8sMetricsClient.METRICS_API_VERSION;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.k8s.metrics.client.model.Usage;
import io.harness.perpetualtask.k8s.metrics.client.model.common.CustomResource;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@TargetModule(Module._420_DELEGATE_AGENT)
public class PodMetrics extends CustomResource {
  @SerializedName("timestamp") private String timestamp;
  @SerializedName("window") private String window;
  @SerializedName("containers") private List<Container> containers;

  @Builder
  public PodMetrics(
      String name, String namespace, String timestamp, String window, @Singular List<Container> containers) {
    this.getMetadata().setName(name);
    this.getMetadata().setNamespace(namespace);
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
  @EqualsAndHashCode
  public static class Container implements Serializable {
    @SerializedName("name") String name;
    @SerializedName("usage") Usage usage;
  }
}
