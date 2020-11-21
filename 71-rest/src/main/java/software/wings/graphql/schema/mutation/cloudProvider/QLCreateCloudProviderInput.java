package software.wings.graphql.schema.mutation.cloudProvider;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLK8sCloudProviderInput;
import software.wings.graphql.schema.type.QLCloudProviderType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLCreateCloudProviderInput implements QLMutationInput {
  private String clientMutationId;

  private QLCloudProviderType cloudProviderType;
  private QLPcfCloudProviderInput pcfCloudProvider;
  private QLSpotInstCloudProviderInput spotInstCloudProvider;
  private QLGcpCloudProviderInput gcpCloudProvider;
  private QLK8sCloudProviderInput k8sCloudProvider;
  private QLPhysicalDataCenterCloudProviderInput physicalDataCenterCloudProvider;
  private QLAzureCloudProviderInput azureCloudProvider;
  private QLAwsCloudProviderInput awsCloudProvider;
}
