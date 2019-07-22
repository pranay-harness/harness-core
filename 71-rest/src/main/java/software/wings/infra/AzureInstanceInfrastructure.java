package software.wings.infra;

import static software.wings.beans.AzureInfrastructureMapping.Builder.anAzureInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureTag;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("AZURE_SSH")
@Data
public class AzureInstanceInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;

  private String subscriptionId;
  private String resourceGroup;
  private List<AzureTag> tags = new ArrayList<>();
  private String hostConnectionAttrs;
  private String winRmConnectionAttributes;
  private boolean usePublicDns;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return anAzureInfrastructureMapping()
        .withComputeProviderSettingId(cloudProviderId)
        .withSubscriptionId(subscriptionId)
        .withResourceGroup(resourceGroup)
        .withTags(tags)
        .withHostConnectionAttributes(hostConnectionAttrs)
        .withWinRmConnectionAttributes(winRmConnectionAttributes)
        .withUsePublicDns(usePublicDns)
        .withResourceGroup(resourceGroup)
        .withInfraMappingType(InfrastructureMappingType.AZURE_INFRA.name())
        .build();
  }

  @Override
  public Class<AzureInfrastructureMapping> getMappingClass() {
    return AzureInfrastructureMapping.class;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AZURE;
  }
}
