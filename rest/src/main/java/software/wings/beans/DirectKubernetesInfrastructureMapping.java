package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Util;

/**
 * Created by brett on 2/27/17
 */
@JsonTypeName("DIRECT_KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
public class DirectKubernetesInfrastructureMapping extends ContainerInfrastructureMapping implements Encryptable {
  @Attributes(title = "Master URL", required = true) @NotEmpty private String masterUrl;
  @Attributes(title = "User Name", required = true) @NotEmpty private String username;
  @Attributes(title = "Password", required = true) @NotEmpty @Encrypted private char[] password;
  @Attributes(title = "Namespace") @NotEmpty private String namespace;

  @SchemaIgnore private String encryptedPassword;

  @SchemaIgnore @Transient private boolean decrypted;

  @Override
  @SchemaIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.DIRECT;
  }

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public DirectKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.DIRECT_KUBERNETES.name());
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ContainerInfrastructureMapping.Yaml {
    private String masterUrl;
    private String username;
    // TODO, use kms
    private char[] password;
    private String namespace;

    public static final class Builder {
      private String cluster;
      private String masterUrl;
      private String username;
      private String computeProviderType;
      // TODO, use kms
      private char[] password;
      private String serviceName;
      private String namespace;
      private String infraMappingType;
      private String type;
      private String deploymentType;
      private String computeProviderName;
      private String name;

      private Builder() {}

      public static Builder aYaml() {
        return new Builder();
      }

      public Builder withCluster(String cluster) {
        this.cluster = cluster;
        return this;
      }

      public Builder withMasterUrl(String masterUrl) {
        this.masterUrl = masterUrl;
        return this;
      }

      public Builder withUsername(String username) {
        this.username = username;
        return this;
      }

      public Builder withComputeProviderType(String computeProviderType) {
        this.computeProviderType = computeProviderType;
        return this;
      }

      public Builder withPassword(char[] password) {
        this.password = password;
        return this;
      }

      public Builder withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
      }

      public Builder withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
      }

      public Builder withInfraMappingType(String infraMappingType) {
        this.infraMappingType = infraMappingType;
        return this;
      }

      public Builder withType(String type) {
        this.type = type;
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

      public Builder but() {
        return aYaml()
            .withCluster(cluster)
            .withMasterUrl(masterUrl)
            .withUsername(username)
            .withComputeProviderType(computeProviderType)
            .withPassword(password)
            .withServiceName(serviceName)
            .withNamespace(namespace)
            .withInfraMappingType(infraMappingType)
            .withType(type)
            .withDeploymentType(deploymentType)
            .withComputeProviderName(computeProviderName)
            .withName(name);
      }

      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setCluster(cluster);
        yaml.setMasterUrl(masterUrl);
        yaml.setUsername(username);
        yaml.setComputeProviderType(computeProviderType);
        yaml.setPassword(password);
        yaml.setServiceName(serviceName);
        yaml.setNamespace(namespace);
        yaml.setInfraMappingType(infraMappingType);
        yaml.setType(type);
        yaml.setDeploymentType(deploymentType);
        yaml.setComputeProviderName(computeProviderName);
        yaml.setName(name);
        return yaml;
      }
    }
  }

  @Override
  @Attributes(title = "Display Name")
  public String getClusterName() {
    return super.getClusterName();
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
    return Util.normalize(getClusterName() + " (Direct Kubernetes)");
  }

  @SchemaIgnore
  public KubernetesConfig createKubernetesConfig() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                            .accountId(getAccountId())
                                            .masterUrl(masterUrl)
                                            .username(username)
                                            .encryptedPassword(encryptedPassword)
                                            .namespace(isNotEmpty(namespace) ? namespace : "default")
                                            .build();
    return kubernetesConfig;
  }

  public Builder deepClone() {
    return aDirectKubernetesInfrastructureMapping()
        .withClusterName(getClusterName())
        .withMasterUrl(getMasterUrl())
        .withUsername(getUsername())
        .withPassword(getPassword())
        .withNamespace(getNamespace())
        .withUuid(getUuid())
        .withComputeProviderSettingId(getComputeProviderSettingId())
        .withAppId(getAppId())
        .withEnvId(getEnvId())
        .withCreatedBy(getCreatedBy())
        .withServiceTemplateId(getServiceTemplateId())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withServiceId(getServiceId())
        .withLastUpdatedAt(getLastUpdatedAt())
        .withComputeProviderType(getComputeProviderType())
        .withInfraMappingType(getInfraMappingType())
        .withEntityYamlPath(entityYamlPath)
        .withDeploymentType(getDeploymentType())
        .withComputeProviderName(getComputeProviderName())
        .withName(getName());
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String clusterName;
    private String masterUrl;
    private String username;
    private char[] password;
    private String namespace;
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

    public Builder withMasterUrl(String masterUrl) {
      this.masterUrl = masterUrl;
      return this;
    }

    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder withPassword(char[] password) {
      this.password = password;
      return this;
    }

    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
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

    public Builder but() {
      return aDirectKubernetesInfrastructureMapping()
          .withClusterName(clusterName)
          .withMasterUrl(masterUrl)
          .withUsername(username)
          .withPassword(password)
          .withNamespace(namespace)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withEntityYamlPath(entityYamlPath)
          .withComputeProviderSettingId(computeProviderSettingId)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceId(serviceId)
          .withComputeProviderType(computeProviderType)
          .withInfraMappingType(infraMappingType)
          .withDeploymentType(deploymentType)
          .withComputeProviderName(computeProviderName)
          .withName(name)
          .withAutoPopulate(autoPopulate);
    }

    public DirectKubernetesInfrastructureMapping build() {
      DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping =
          new DirectKubernetesInfrastructureMapping();
      directKubernetesInfrastructureMapping.setClusterName(clusterName);
      directKubernetesInfrastructureMapping.setMasterUrl(masterUrl);
      directKubernetesInfrastructureMapping.setUsername(username);
      directKubernetesInfrastructureMapping.setPassword(password);
      directKubernetesInfrastructureMapping.setNamespace(namespace);
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
      return directKubernetesInfrastructureMapping;
    }
  }
}
