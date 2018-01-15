package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by brett on 2/27/17
 */
@JsonTypeName("DIRECT_KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
public class DirectKubernetesInfrastructureMapping extends ContainerInfrastructureMapping implements Encryptable {
  @Attributes(title = "Master URL", required = true) @NotEmpty private String masterUrl;
  @Attributes(title = "User Name") private String username;
  @Encrypted @Attributes(title = "Password") private char[] password;
  @Encrypted @Attributes(title = "CA Certificate") private char[] caCert;
  @Encrypted @Attributes(title = "Client Certificate") private char[] clientCert;
  @Encrypted @Attributes(title = "Client Key") private char[] clientKey;
  @Attributes(title = "Namespace") private String namespace;

  @SchemaIgnore private String encryptedPassword;
  @SchemaIgnore private String encryptedCaCert;
  @SchemaIgnore private String encryptedClientCert;
  @SchemaIgnore private String encryptedClientKey;

  @SchemaIgnore @Transient private boolean decrypted;

  @Override
  @SchemaIgnore
  @JsonIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.DIRECT;
  }

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public DirectKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.DIRECT_KUBERNETES.name());
  }

  @SchemaIgnore
  @Override
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
    StringBuilder nameBuilder = new StringBuilder(128);
    try {
      URL url = new URL(getMasterUrl());
      if (url.getHost() != null) {
        String hostName = url.getHost().replace('.', '_');
        nameBuilder.append(hostName);
      } else {
        nameBuilder.append(getMasterUrl());
      }
    } catch (MalformedURLException e) {
      nameBuilder.append(getMasterUrl());
    }

    if (getUsername() != null) {
      nameBuilder.append('_');
      nameBuilder.append(getUsername());
    }

    nameBuilder.append(" (Direct Kubernetes)");

    return Util.normalize(nameBuilder.toString());
  }

  @SchemaIgnore
  public KubernetesConfig createKubernetesConfig() {
    return KubernetesConfig.builder()
        .accountId(getAccountId())
        .masterUrl(masterUrl)
        .username(username)
        .encryptedPassword(encryptedPassword)
        .encryptedCaCert(encryptedCaCert)
        .encryptedClientCert(encryptedClientCert)
        .encryptedClientKey(encryptedClientKey)
        .namespace(isNotEmpty(namespace) ? namespace : "default")
        .build();
  }

  public Builder deepClone() {
    return aDirectKubernetesInfrastructureMapping()
        .withClusterName(getClusterName())
        .withMasterUrl(getMasterUrl())
        .withUsername(getUsername())
        .withPassword(getPassword())
        .withCaCert(getCaCert())
        .withClientCert(getClientCert())
        .withClientKey(getClientKey())
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
    private String accountId;
    private String clusterName;
    private String masterUrl;
    private String username;
    private char[] password;
    private char[] caCert;
    private char[] clientCert;
    private char[] clientKey;
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

    public Builder withCaCert(char[] caCert) {
      this.caCert = caCert;
      return this;
    }

    public Builder withClientCert(char[] clientCert) {
      this.clientCert = clientCert;
      return this;
    }

    public Builder withClientKey(char[] clientKey) {
      this.clientKey = clientKey;
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

    public Builder but() {
      return aDirectKubernetesInfrastructureMapping()
          .withClusterName(clusterName)
          .withMasterUrl(masterUrl)
          .withUsername(username)
          .withPassword(password)
          .withCaCert(caCert)
          .withClientCert(clientCert)
          .withClientKey(clientKey)
          .withNamespace(namespace)
          .withUuid(uuid)
          .withAppId(appId)
          .withAccountId(accountId)
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
      directKubernetesInfrastructureMapping.setCaCert(caCert);
      directKubernetesInfrastructureMapping.setClientCert(clientCert);
      directKubernetesInfrastructureMapping.setClientKey(clientKey);
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
      directKubernetesInfrastructureMapping.setAccountId(accountId);
      return directKubernetesInfrastructureMapping;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ContainerInfrastructureMapping.Yaml {
    private String masterUrl;
    private String username;
    private String password;
    private String caCert;
    private String clientCert;
    private String clientKey;
    private String namespace;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String cluster, String masterUrl, String username, String password, String caCert,
        String clientCert, String clientKey, String namespace) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, cluster);
      this.masterUrl = masterUrl;
      this.username = username;
      this.password = password;
      this.caCert = caCert;
      this.clientCert = clientCert;
      this.clientKey = clientKey;
      this.namespace = namespace;
    }
  }
}
