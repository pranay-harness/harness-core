package software.wings.beans;

import static java.lang.String.format;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import software.wings.utils.Utils;

import java.util.Map;
import java.util.Optional;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureAppServiceInfrastructureMappingKeys")
public abstract class AzureAppServiceInfrastructureMapping extends InfrastructureMapping {
  @Trimmed @Attributes(title = "SubscriptionId") @Getter @Setter private String subscriptionId;
  @Trimmed @Attributes(title = "Resource Group") @Getter @Setter private String resourceGroup;
  @Trimmed @Attributes(title = "Deployment Slot") @Getter @Setter private String deploymentSlot;

  public AzureAppServiceInfrastructureMapping(String entityYamlPath, String appId, String accountId, String type,
      String uuid, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String computeProviderSettingId, String envId, String serviceTemplateId, String serviceId,
      String computeProviderType, String infraMappingType, String deploymentType, String computeProviderName,
      String name, boolean autoPopulateName, Map<String, Object> blueprints, String provisionerId, boolean sample,
      String subscriptionId, String resourceGroup, String deploymentSlot) {
    super(entityYamlPath, appId, accountId, type, uuid, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt,
        computeProviderSettingId, envId, serviceTemplateId, serviceId, computeProviderType, infraMappingType,
        deploymentType, computeProviderName, name, autoPopulateName, blueprints, provisionerId, sample);
    this.subscriptionId = subscriptionId;
    this.resourceGroup = resourceGroup;
    this.deploymentSlot = deploymentSlot;
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map,
      InfrastructureMappingBlueprint.NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    throw new UnsupportedOperationException();
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("(%s)_%s", getAppServiceType(),
        Optional.ofNullable(getComputeProviderName()).orElse(getComputeProviderType().toLowerCase())));
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  protected abstract String getAppServiceType();

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class AppServiceYaml extends InfrastructureMapping.YamlWithComputeProvider {
    private String subscriptionId;
    private String resourceGroup;
    private String deploymentSlot;

    public AppServiceYaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, Map<String, Object> blueprints,
        String subscriptionId, String resourceGroup, String deploymentSlot) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
          computeProviderName, blueprints);
      this.subscriptionId = subscriptionId;
      this.resourceGroup = resourceGroup;
      this.deploymentSlot = deploymentSlot;
    }
  }
}
