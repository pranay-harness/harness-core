package software.wings.beans;

import static software.wings.beans.CloudFormationSourceType.TEMPLATE_BODY;
import static software.wings.beans.CloudFormationSourceType.TEMPLATE_URL;
import static software.wings.beans.InfrastructureProvisionerType.CLOUD_FORMATION;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
}