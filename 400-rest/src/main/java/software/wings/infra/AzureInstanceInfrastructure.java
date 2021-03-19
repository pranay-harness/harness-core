package software.wings.infra;

import static software.wings.beans.AzureInfrastructureMapping.Builder.anAzureInfrastructureMapping;
import static software.wings.beans.InfrastructureType.AZURE_SSH;

import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureTag;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@JsonTypeName("AZURE_INFRA")
@Data
@Builder
public class AzureInstanceInfrastructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider,
                                                    SshBasedInfrastructure, WinRmBasedInfrastructure {
  private String cloudProviderId;
  @IncludeFieldMap private String subscriptionId;
  @IncludeFieldMap private String resourceGroup;
  @Builder.Default private List<AzureTag> tags = new ArrayList<>();
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

  @Override
  public String getInfrastructureType() {
    return AZURE_SSH;
  }
}
