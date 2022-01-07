/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;

import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("AZURE_KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureKubernetesInfrastructureMappingKeys")
@TargetModule(_957_CG_BEANS)
@OwnedBy(CDP)
public class AzureKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  @Attributes(title = "SubscriptionId") private String subscriptionId;
  @Attributes(title = "Resource Group") private String resourceGroup;
  @Attributes(title = "Namespace") private String namespace;
  @Trimmed private String releaseName;
  private String masterUrl;

  public AzureKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.AZURE_KUBERNETES.name());
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    throw new UnsupportedOperationException();
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("%s_AZURE_Kubernetes_%s_%s", this.getClusterName(),
        Optional.ofNullable(this.getComputeProviderName())
            .orElse(this.getComputeProviderType().toLowerCase())
            .replace(':', '_'),
        Optional.ofNullable(this.getNamespace()).orElse("default")));
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String clusterName;
    private String masterUrl;
    private String subscriptionId;
    private String resourceGroup;
    private String namespace;
    private String releaseName;
    private String uuid;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String infraMappingType;
    private String deploymentType;
    private String computeProviderName;
    private String name;
    // auto populate name
    private boolean autoPopulate = true;

    private Builder() {}

    public static Builder anAzureKubernetesInfrastructureMapping() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withMasterUrl(String masterUrl) {
      this.masterUrl = masterUrl;
      return this;
    }

    public Builder withSubscriptionId(String subscriptionId) {
      this.subscriptionId = subscriptionId;
      return this;
    }

    public Builder withResourceGroup(String resourceGroup) {
      this.resourceGroup = resourceGroup;
      return this;
    }

    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder withReleaseName(String releaseName) {
      this.releaseName = releaseName;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public AzureKubernetesInfrastructureMapping build() {
      AzureKubernetesInfrastructureMapping azureKubernetesInfrastructureMapping =
          new AzureKubernetesInfrastructureMapping();
      azureKubernetesInfrastructureMapping.setClusterName(clusterName);
      azureKubernetesInfrastructureMapping.setSubscriptionId(subscriptionId);
      azureKubernetesInfrastructureMapping.setResourceGroup(resourceGroup);
      azureKubernetesInfrastructureMapping.setNamespace(namespace);
      azureKubernetesInfrastructureMapping.setReleaseName(releaseName);
      azureKubernetesInfrastructureMapping.setUuid(uuid);
      azureKubernetesInfrastructureMapping.setAppId(appId);
      azureKubernetesInfrastructureMapping.setCreatedBy(createdBy);
      azureKubernetesInfrastructureMapping.setCreatedAt(createdAt);
      azureKubernetesInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      azureKubernetesInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      azureKubernetesInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      azureKubernetesInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      azureKubernetesInfrastructureMapping.setEnvId(envId);
      azureKubernetesInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      azureKubernetesInfrastructureMapping.setServiceId(serviceId);
      azureKubernetesInfrastructureMapping.setComputeProviderType(computeProviderType);
      azureKubernetesInfrastructureMapping.setInfraMappingType(infraMappingType);
      azureKubernetesInfrastructureMapping.setDeploymentType(deploymentType);
      azureKubernetesInfrastructureMapping.setComputeProviderName(computeProviderName);
      azureKubernetesInfrastructureMapping.setName(name);
      azureKubernetesInfrastructureMapping.setAutoPopulate(autoPopulate);
      azureKubernetesInfrastructureMapping.setAccountId(accountId);
      azureKubernetesInfrastructureMapping.setMasterUrl(masterUrl);
      return azureKubernetesInfrastructureMapping;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends YamlWithComputeProvider {
    private String subscriptionId;
    private String resourceGroup;
    private String namespace;
    private String releaseName;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String cluster,
        String subscriptionId, String resourceGroup, String namespace, String releaseName,
        Map<String, Object> blueprints) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, cluster, blueprints);
      this.subscriptionId = subscriptionId;
      this.resourceGroup = resourceGroup;
      this.namespace = namespace;
      this.releaseName = releaseName;
    }
  }
}
