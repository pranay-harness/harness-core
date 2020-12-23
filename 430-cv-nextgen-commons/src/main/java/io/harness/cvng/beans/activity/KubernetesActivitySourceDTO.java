package io.harness.cvng.beans.activity;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("KUBERNETES")
public class KubernetesActivitySourceDTO extends ActivitySourceDTO {
  @NotNull String connectorIdentifier;
  @NotNull @NotEmpty Set<KubernetesActivitySourceConfig> activitySourceConfigs;

  @Override
  public ActivitySourceType getType() {
    return ActivitySourceType.KUBERNETES;
  }

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "KubernetesActivitySourceConfigKeys")
  public static class KubernetesActivitySourceConfig {
    @NotNull String serviceIdentifier;
    @NotNull String envIdentifier;
    @NotNull String namespace;
    @NotNull String workloadName;
  }
}
