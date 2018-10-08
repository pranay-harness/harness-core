package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.settings.SettingValue.SettingVariableTypes.KUBERNETES_CLUSTER;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.annotation.Encrypted;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.beans.KubernetesConfig.KubernetesConfigBuilder;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

/**
 * Created by brett on 2/27/17
 */
@JsonTypeName("DIRECT_KUBERNETES")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class DirectKubernetesInfrastructureMapping
    extends ContainerInfrastructureMapping implements EncryptableSetting {
  @Attributes(title = "Master URL") private String masterUrl;
  @Attributes(title = "User Name") private String username;
  @Encrypted @Attributes(title = "Password") private char[] password;
  @Encrypted @Attributes(title = "CA Certificate") private char[] caCert;
  @Encrypted @Attributes(title = "Client Certificate") private char[] clientCert;
  @Encrypted @Attributes(title = "Client Key") private char[] clientKey;
  @Encrypted @Attributes(title = "Client Key Passphrase") private char[] clientKeyPassphrase;
  @Encrypted @Attributes(title = "Service Account Token") private char[] serviceAccountToken;
  @Attributes(title = "Client Key Algorithm") private String clientKeyAlgo;
  @Attributes(title = "Namespace") private String namespace;

  @SchemaIgnore private String encryptedPassword;
  @SchemaIgnore private String encryptedCaCert;
  @SchemaIgnore private String encryptedClientCert;
  @SchemaIgnore private String encryptedClientKey;
  @SchemaIgnore private String encryptedClientKeyPassphrase;
  @SchemaIgnore private String encryptedServiceAccountToken;

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
    if (getComputeProviderType().equals(SettingVariableTypes.DIRECT.name())) {
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
    } else {
      return Util.normalize(format("%s_DIRECT_Kubernetes_%s",
          Optional.ofNullable(getComputeProviderName())
              .orElse(getComputeProviderType().toLowerCase())
              .replace(':', '_'),
          Optional.ofNullable(getNamespace()).orElse("default")));
    }
  }

  @SchemaIgnore
  public KubernetesConfig createKubernetesConfig() {
    KubernetesConfigBuilder kubernetesConfig = KubernetesConfig.builder()
                                                   .accountId(getAccountId())
                                                   .masterUrl(masterUrl)
                                                   .username(username)
                                                   .clientKeyAlgo(clientKeyAlgo)
                                                   .namespace(isNotEmpty(namespace) ? namespace : "default");
    if (isNotBlank(encryptedPassword)) {
      kubernetesConfig.encryptedPassword(encryptedPassword);
    } else {
      kubernetesConfig.password(password);
    }

    if (isNotBlank(encryptedCaCert)) {
      kubernetesConfig.encryptedCaCert(encryptedCaCert);
    } else {
      kubernetesConfig.caCert(caCert);
    }

    if (isNotBlank(encryptedClientCert)) {
      kubernetesConfig.encryptedClientCert(encryptedClientCert);
    } else {
      kubernetesConfig.clientCert(clientCert);
    }

    if (isNotBlank(encryptedClientKey)) {
      kubernetesConfig.encryptedClientKey(encryptedClientKey);
    } else {
      kubernetesConfig.clientKey(clientKey);
    }

    if (isNotBlank(encryptedClientKeyPassphrase)) {
      kubernetesConfig.encryptedClientKeyPassphrase(encryptedClientKeyPassphrase);
    } else {
      kubernetesConfig.clientKeyPassphrase(clientKeyPassphrase);
    }

    if (isNotBlank(encryptedServiceAccountToken)) {
      kubernetesConfig.encryptedServiceAccountToken(encryptedServiceAccountToken);
    } else {
      kubernetesConfig.serviceAccountToken(serviceAccountToken);
    }

    return kubernetesConfig.build();
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
    private char[] clientKeyPassphrase;
    private char[] serviceAccountToken;
    private String clientKeyAlgo;
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

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withPassword(char[] password) {
      this.password = password;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withCaCert(char[] caCert) {
      this.caCert = caCert;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withClientCert(char[] clientCert) {
      this.clientCert = clientCert;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withClientKey(char[] clientKey) {
      this.clientKey = clientKey;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withClientKeyPassphrase(char[] clientKeyPassphrase) {
      this.clientKeyPassphrase = clientKeyPassphrase;
      return this;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public Builder withServiceAccountToken(char[] serviceAccountToken) {
      this.serviceAccountToken = serviceAccountToken;
      return this;
    }

    public Builder withClientKeyAlgo(String clientKeyAlgo) {
      this.clientKeyAlgo = clientKeyAlgo;
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
      directKubernetesInfrastructureMapping.setClientKeyPassphrase(clientKeyPassphrase);
      directKubernetesInfrastructureMapping.setServiceAccountToken(serviceAccountToken);
      directKubernetesInfrastructureMapping.setClientKeyAlgo(clientKeyAlgo);
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

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String cluster, String masterUrl,
        String username, String password, String caCert, String clientCert, String clientKey,
        String clientKeyPassphrase, String serviceAccountToken, String clientKeyAlgo, String namespace) {
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
    }
  }
}
