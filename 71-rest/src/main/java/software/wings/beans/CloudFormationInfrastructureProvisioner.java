package software.wings.beans;

import static software.wings.beans.CloudFormationSourceType.GIT;
import static software.wings.beans.CloudFormationSourceType.TEMPLATE_BODY;
import static software.wings.beans.CloudFormationSourceType.TEMPLATE_URL;
import static software.wings.beans.InfrastructureProvisionerType.CLOUD_FORMATION;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("CLOUD_FORMATION")
public class CloudFormationInfrastructureProvisioner extends InfrastructureProvisioner {
  private static String VARIABLE_KEY = "cloudformation";
  @NotEmpty private String sourceType;
  private String templateBody;
  private String templateFilePath;
  private GitFileConfig gitFileConfig;

  public boolean provisionByBody() {
    return TEMPLATE_BODY.name().equals(sourceType);
  }
  public boolean provisionByUrl() {
    return TEMPLATE_URL.name().equals(sourceType);
  }
  public boolean provisionByGit() {
    return GIT.name().equals(sourceType);
  }

  @Builder
  private CloudFormationInfrastructureProvisioner(String uuid, String appId, String name, String awsConfigId,
      String sourceType, String templateBody, String templateFilePath, GitFileConfig gitFileConfig,
      List<NameValuePair> variables, List<InfrastructureMappingBlueprint> mappingBlueprints, String accountId,
      String provisionerTemplateData, String stackName, String description, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath) {
    super(name, description, CLOUD_FORMATION.name(), variables, mappingBlueprints, accountId, uuid, appId, createdBy,
        createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    setSourceType(sourceType);
    setTemplateBody(templateBody);
    setGitFileConfig(gitFileConfig);
    setTemplateFilePath(templateFilePath);
  }

  CloudFormationInfrastructureProvisioner() {
    setInfrastructureProvisionerType(CLOUD_FORMATION.name());
  }

  @Override
  public String variableKey() {
    return VARIABLE_KEY;
  }
  /**
   * The type Yaml.
   */
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class Yaml extends InfraProvisionerYaml {
    private String sourceType;
    private String templateBody;
    private String templateFilePath;
    private GitFileConfig gitFileConfig;

    // TODO: check usage of yaml constructor
    @Builder
    public Yaml(String type, String harnessApiVersion, String description, String infrastructureProvisionerType,
        List<NameValuePair.Yaml> variables, List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints,
        String sourceType, String templateBody, String templateFilePath, GitFileConfig gitFileConfig) {
      super(type, harnessApiVersion, description, infrastructureProvisionerType, variables, mappingBlueprints);
      this.sourceType = sourceType;
      this.templateBody = templateBody;
      this.templateFilePath = templateFilePath;
      this.gitFileConfig = gitFileConfig;
    }
  }
}