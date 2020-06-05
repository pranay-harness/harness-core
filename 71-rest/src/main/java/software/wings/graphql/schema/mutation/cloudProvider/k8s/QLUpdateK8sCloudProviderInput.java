package software.wings.graphql.schema.mutation.cloudProvider.k8s;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.cloudProvider.k8s.QLClusterDetailsType;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateK8sCloudProviderInput {
  private RequestField<String> name;
  private RequestField<QLUsageScope> usageScope;

  private RequestField<QLClusterDetailsType> clusterDetailsType;
  private RequestField<QLUpdateInheritClusterDetails> inheritClusterDetails;
  private RequestField<QLUpdateManualClusterDetails> manualClusterDetails;

  private RequestField<Boolean> skipValidation;
}
