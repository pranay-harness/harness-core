package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonTypeName("AZURE_INFRA")
public class AzureInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Subscription Id") private String subscriptionId;
  @Attributes(title = "Resource Group") private String resourceGroup;
  @Attributes(title = "Tags") private List<AzureTag> tags = new ArrayList<>();
  private String hostConnectionAttrs;
  private String winRmConnectionAttributes;
  private boolean usePublicDns;

  public String getWinRmConnectionAttributes() {
    return winRmConnectionAttributes;
  }
  public void setWinRmConnectionAttributes(String winRmConnectionAttributes) {
    this.winRmConnectionAttributes = winRmConnectionAttributes;
  }

  public AzureInfrastructureMapping() {
    super(InfrastructureMappingType.AZURE_INFRA.name());
  }
  @Override
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }
  public void setHostConnectionAttrs(String hostConnectionAttrs) {
    this.hostConnectionAttrs = hostConnectionAttrs;
  }
  public String getSubscriptionId() {
    return subscriptionId;
  }
  public void setSubscriptionId(String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }
  public String getResourceGroup() {
    return resourceGroup;
  }
  public void setResourceGroup(String resourceGroup) {
    this.resourceGroup = resourceGroup;
  }
  public List<AzureTag> getTags() {
    return tags;
  }
  public void setTags(List<AzureTag> tags) {
    this.tags = tags;
  }
  public boolean isUsePublicDns() {
    return this.usePublicDns;
  }
  public void setUsePublicDns(boolean usePublicDns) {
    this.usePublicDns = usePublicDns;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    List<String> parts = new ArrayList();
    if (isNotEmpty(getComputeProviderName())) {
      parts.add(getComputeProviderName().toLowerCase());
    }

    parts.add("AZURE");

    parts.add(getDeploymentType());
    return Util.normalize(String.join(" - ", parts));
  }
  @Override
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {}
}
