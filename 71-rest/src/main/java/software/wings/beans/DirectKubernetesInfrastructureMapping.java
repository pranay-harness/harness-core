package software.wings.beans;

import static java.lang.String.format;
import static software.wings.settings.SettingValue.SettingVariableTypes.KUBERNETES_CLUSTER;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Util;

import java.util.Map;
import java.util.Optional;

/**
 * Created by brett on 2/27/17
 */
@JsonTypeName("DIRECT_KUBERNETES")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class DirectKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  @Attributes(title = "Namespace") private String namespace;
  @Trimmed private String releaseName;

  public DirectKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.DIRECT_KUBERNETES.name());
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {}

  @SchemaIgnore
  @Override
  public String getClusterName() {
    return KUBERNETES_CLUSTER.name().equals(getComputeProviderType()) ? getComputeProviderName()
                                                                      : super.getClusterName();
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(format("%s_DIRECT_Kubernetes_%s",
        Optional.ofNullable(getComputeProviderName()).orElse(getComputeProviderType().toLowerCase()).replace(':', '_'),
        Optional.ofNullable(getNamespace()).orElse("default")));
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String clusterName;
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

    public static Builder aDirectKubernetesInfrastructureMapping() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
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

    public DirectKubernetesInfrastructureMapping build() {
      DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping =
          new DirectKubernetesInfrastructureMapping();
      directKubernetesInfrastructureMapping.setClusterName(clusterName);
      directKubernetesInfrastructureMapping.setNamespace(namespace);
      directKubernetesInfrastructureMapping.setReleaseName(releaseName);
      directKubernetesInfrastructureMapping.setUuid(uuid);
      directKubernetesInfrastructureMapping.setAppId(appId);
      directKubernetesInfrastructureMapping.setCreatedBy(createdBy);
      directKubernetesInfrastructureMapping.setCreatedAt(createdAt);
      directKubernetesInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      directKubernetesInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      directKubernetesInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      directKubernetesInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      directKubernetesInfrastructureMapping.setEnvId(envId);
      directKubernetesInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      directKubernetesInfrastructureMapping.setServiceId(serviceId);
      directKubernetesInfrastructureMapping.setComputeProviderType(computeProviderType);
      directKubernetesInfrastructureMapping.setInfraMappingType(infraMappingType);
      directKubernetesInfrastructureMapping.setDeploymentType(deploymentType);
      directKubernetesInfrastructureMapping.setComputeProviderName(computeProviderName);
      directKubernetesInfrastructureMapping.setName(name);
      directKubernetesInfrastructureMapping.setAutoPopulate(autoPopulate);
      directKubernetesInfrastructureMapping.setAccountId(accountId);
      return directKubernetesInfrastructureMapping;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends YamlWithComputeProvider {
    private String masterUrl;
    private String username;
    private String password;
    private String caCert;
    private String clientCert;
    private String clientKey;
    private String clientKeyPassphrase;
    private String serviceAccountToken;
    private String clientKeyAlgo;
    private String namespace;
    private String releaseName;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String cluster, String masterUrl,
        String username, String password, String caCert, String clientCert, String clientKey,
        String clientKeyPassphrase, String serviceAccountToken, String clientKeyAlgo, String namespace,
        String releaseName) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName, cluster);
      this.masterUrl = masterUrl;
      this.username = username;
      this.password = password;
      this.caCert = caCert;
      this.clientCert = clientCert;
      this.clientKey = clientKey;
      this.clientKeyPassphrase = clientKeyPassphrase;
      this.serviceAccountToken = serviceAccountToken;
      this.clientKeyAlgo = clientKeyAlgo;
      this.namespace = namespace;
      this.releaseName = releaseName;
    }
  }
}
