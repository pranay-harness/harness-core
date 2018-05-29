package software.wings.beans;

import static software.wings.beans.CloudFormationSourceType.TEMPLATE_BODY;
import static software.wings.beans.CloudFormationSourceType.TEMPLATE_URL;
import static software.wings.beans.InfrastructureProvisionerType.CLOUD_FORMATION;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CLOUD_FORMATION")
public class CloudFormationInfrastructureProvisioner extends InfrastructureProvisioner {
  private static String VARIABLE_KEY = "cloudformation";
  @NotEmpty private String sourceType;
  private String templateBody;
  private String templateFilePath;

  public boolean provisionByBody() {
    return TEMPLATE_BODY.name().equals(sourceType);
  }
  public boolean provisionByUrl() {
    return TEMPLATE_URL.name().equals(sourceType);
  }

  @Builder
  private CloudFormationInfrastructureProvisioner(String uuid, String appId, String name, String awsConfigId,
      String sourceType, String templateBody, String templateFilePath, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints, String provisionerTemplateData, String stackName,
      String description, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      List<String> keywords, String entityYamlPath) {
    super(name, description, CLOUD_FORMATION.name(), variables, mappingBlueprints, uuid, appId, createdBy, createdAt,
        lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    setSourceType(sourceType);
    setTemplateBody(templateBody);
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
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @NoArgsConstructor
  public static final class Yaml extends InfrastructureProvisioner.Yaml {
    private String sourceType;
    private String templateBody;
    private String templateFilePath;

    @Builder
    public Yaml(String type, String harnessApiVersion, String name, String description,
        String infrastructureProvisionerType, List<NameValuePair.Yaml> variables,
        List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints, String sourceType, String templateBody,
        String templateFilePath) {
      super(type, harnessApiVersion, name, description, infrastructureProvisionerType, variables, mappingBlueprints);
      this.sourceType = sourceType;
      this.templateBody = templateBody;
      this.templateFilePath = templateFilePath;
    }
  }
}