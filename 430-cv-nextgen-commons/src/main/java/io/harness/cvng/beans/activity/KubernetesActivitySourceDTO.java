package io.harness.cvng.beans.activity;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class KubernetesActivitySourceDTO {
  String uuid;
  @NotNull String identifier;
  @NotNull String name;
  @NotNull String connectorIdentifier;
  @NotNull @NotEmpty Set<KubernetesActivitySourceConfig> activitySourceConfigs;

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "KubernetesActivitySourceConfigKeys")
  public static class KubernetesActivitySourceConfig {
    @NotNull String serviceIdentifier;
    @NotNull String envIdentifier;
    @NotNull String namespace;
    @NotNull String workloadName;
  }
}
