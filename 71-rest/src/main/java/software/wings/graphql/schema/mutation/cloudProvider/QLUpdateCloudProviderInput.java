package software.wings.graphql.schema.mutation.cloudProvider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLK8sCloudProviderInput;
import software.wings.graphql.schema.type.QLCloudProviderType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateCloudProviderInput implements QLMutationInput {
  private String clientMutationId;

  private String cloudProviderId;
  private QLCloudProviderType cloudProviderType;
  private QLPcfCloudProviderInput pcfCloudProvider;
  private QLSpotInstCloudProviderInput spotInstCloudProvider;
  private QLGcpCloudProviderInput gcpCloudProvider;
  private QLK8sCloudProviderInput k8sCloudProvider;
  private QLPhysicalDataCenterCloudProviderInput physicalDataCenterCloudProvider;
  private QLAzureCloudProviderInput azureCloudProvider;
}
