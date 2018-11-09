package software.wings.beans;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.stencils.EnumData;
import software.wings.utils.Util;

import java.util.Map;
import java.util.Optional;

/**
 * Created by brett on 6/22/17
 */
@JsonTypeName("AWS_AWS_CODEDEPLOY")
public class CodeDeployInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Region", required = true)
  @NotEmpty
  @EnumData(enumDataProvider = AwsInfrastructureMapping.AwsRegionDataProvider.class)
  private String region;
  @Attributes(title = "Application Name", required = true) @NotEmpty private String applicationName;
  @Attributes(title = "Deployment Group", required = true) @NotEmpty private String deploymentGroup;
  @Attributes(title = "Deployment Configuration") private String deploymentConfig;
  @Attributes(title = "AWS Host Name Convention") private String hostNameConvention;

  /**
   * Instantiates a new Aws CodeDeploy infrastructure mapping.
   */
  public CodeDeployInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name());
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {}

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends InfrastructureMapping.YamlWithComputeProvider {
    private String region;
    private String applicationName;
    private String deploymentGroup;
    private String deploymentConfig;
    private String hostNameConvention;

    @Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String region,
        String applicationName, String deploymentGroup, String deploymentConfig, String hostNameConvention) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName);
      this.region = region;
      this.applicationName = applicationName;
      this.deploymentGroup = deploymentGroup;
      this.deploymentConfig = deploymentConfig;
      this.hostNameConvention = hostNameConvention;
    }
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
    return Util.normalize(format("%s (AWS/CodeDeploy) %s",
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getRegion()));
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  public String getDeploymentGroup() {
    return deploymentGroup;
  }

  public void setDeploymentGroup(String deploymentGroup) {
    this.deploymentGroup = deploymentGroup;
  }

  public String getDeploymentConfig() {
    return deploymentConfig;
  }

  public void setDeploymentConfig(String deploymentConfig) {
    this.deploymentConfig = deploymentConfig;
  }

  public String getHostNameConvention() {
    return hostNameConvention;
  }

  public void setHostNameConvention(String hostNameConvention) {
    this.hostNameConvention = hostNameConvention;
  }

  public static final class CodeDeployInfrastructureMappingBuilder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String region;
    private String applicationName;
    private String deploymentGroup;
    private String deploymentConfig;
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
    private String hostNameConvention;
    // auto populate name
    private boolean autoPopulate = true;

    private CodeDeployInfrastructureMappingBuilder() {}

    public static CodeDeployInfrastructureMappingBuilder aCodeDeployInfrastructureMapping() {
      return new CodeDeployInfrastructureMappingBuilder();
    }

    public CodeDeployInfrastructureMappingBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withApplicationName(String applicationName) {
      this.applicationName = applicationName;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withDeploymentGroup(String deploymentGroup) {
      this.deploymentGroup = deploymentGroup;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withDeploymentConfig(String deploymentConfig) {
      this.deploymentConfig = deploymentConfig;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public CodeDeployInfrastructureMappingBuilder withHostNameConvention(String hostNameConvention) {
      this.hostNameConvention = hostNameConvention;
      return this;
    }

    public CodeDeployInfrastructureMapping build() {
      CodeDeployInfrastructureMapping codeDeployInfrastructureMapping = new CodeDeployInfrastructureMapping();
      codeDeployInfrastructureMapping.setRegion(region);
      codeDeployInfrastructureMapping.setApplicationName(applicationName);
      codeDeployInfrastructureMapping.setDeploymentGroup(deploymentGroup);
      codeDeployInfrastructureMapping.setDeploymentConfig(deploymentConfig);
      codeDeployInfrastructureMapping.setUuid(uuid);
      codeDeployInfrastructureMapping.setAppId(appId);
      codeDeployInfrastructureMapping.setCreatedBy(createdBy);
      codeDeployInfrastructureMapping.setCreatedAt(createdAt);
      codeDeployInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      codeDeployInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      codeDeployInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      codeDeployInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      codeDeployInfrastructureMapping.setEnvId(envId);
      codeDeployInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      codeDeployInfrastructureMapping.setServiceId(serviceId);
      codeDeployInfrastructureMapping.setComputeProviderType(computeProviderType);
      codeDeployInfrastructureMapping.setInfraMappingType(infraMappingType);
      codeDeployInfrastructureMapping.setDeploymentType(deploymentType);
      codeDeployInfrastructureMapping.setComputeProviderName(computeProviderName);
      codeDeployInfrastructureMapping.setName(name);
      codeDeployInfrastructureMapping.setAutoPopulate(autoPopulate);
      codeDeployInfrastructureMapping.setAccountId(accountId);
      codeDeployInfrastructureMapping.setHostNameConvention(hostNameConvention);
      return codeDeployInfrastructureMapping;
    }
  }
}
