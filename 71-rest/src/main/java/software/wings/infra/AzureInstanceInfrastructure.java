package software.wings.infra;

import static software.wings.beans.AzureInfrastructureMapping.Builder.anAzureInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureInfrastructureMapping.AzureInfrastructureMappingKeys;
import software.wings.beans.AzureTag;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import java.util.ArrayList;
import java.util.List;

@JsonTypeName("AZURE_SSH")
@Data
public class AzureInstanceInfrastructure implements InfraMappingInfrastructureProvider {
  private String cloudProviderId;

  @IncludeInFieldMap(key = AzureInfrastructureMappingKeys.subscriptionId) private String subscriptionId;
  @IncludeInFieldMap(key = AzureInfrastructureMappingKeys.resourceGroup) private String resourceGroup;
  @IncludeInFieldMap(key = AzureInfrastructureMappingKeys.tags) private List<AzureTag> tags = new ArrayList<>();
  @IncludeInFieldMap(key = AzureInfrastructureMappingKeys.hostConnectionAttrs) private String hostConnectionAttrs;
  @IncludeInFieldMap(key = AzureInfrastructureMappingKeys.winRmConnectionAttributes)
  private String winRmConnectionAttributes;
  @IncludeInFieldMap(key = AzureInfrastructureMappingKeys.usePublicDns) private boolean usePublicDns;

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
}
