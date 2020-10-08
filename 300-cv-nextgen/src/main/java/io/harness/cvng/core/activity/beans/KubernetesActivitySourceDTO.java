package io.harness.cvng.core.activity.beans;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

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
