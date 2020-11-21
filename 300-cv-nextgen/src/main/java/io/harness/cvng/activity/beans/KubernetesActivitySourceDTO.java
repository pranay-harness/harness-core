package io.harness.cvng.activity.beans;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KubernetesActivitySourceDTO {
  String uuid;
  @NotNull String connectorIdentifier;
  @NotNull String serviceIdentifier;
  @NotNull String envIdentifier;
  @NotNull String namespace;
  @NotNull String clusterName;
  String workloadName;
}
