package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.data.validator.Trimmed;
import lombok.Data;

@JsonTypeName("DIRECT_KUBERNETES")
@Data
public class DirectKubernetesInfrastructure implements KubernetesInfrastructure, CloudProviderInfrastructure {
  private String cloudProviderId;
  private String clusterName;
  private String namespace;
  @Trimmed private String releaseName;
}
