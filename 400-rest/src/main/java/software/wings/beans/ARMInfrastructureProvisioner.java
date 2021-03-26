package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.InfrastructureProvisionerType.ARM;

import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMResourceType;
import io.harness.azure.model.ARMScopeType;
import io.harness.beans.EmbeddedUser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("ARM")
@OwnedBy(CDP)
public class ARMInfrastructureProvisioner extends InfrastructureProvisioner {
  private static String VARIABLE_KEY = "arm";
  private ARMResourceType resourceType;
  private ARMSourceType sourceType;
  private ARMScopeType scopeType;
  private String templateBody;
  private GitFileConfig gitFileConfig;

  @Builder
  private ARMInfrastructureProvisioner(String name, String description, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints, String accountId, String uuid, String appId,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath,
      ARMSourceType sourceType, String templateBody, GitFileConfig gitFileConfig, ARMScopeType scopeType,
      ARMResourceType resourceType) {
    super(name, description, ARM.name(), variables, mappingBlueprints, accountId, uuid, appId, createdBy, createdAt,
        lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.sourceType = sourceType;
    this.templateBody = trim(templateBody);
    this.gitFileConfig = gitFileConfig;
    this.scopeType = scopeType;
    this.resourceType = resourceType;
  }

  ARMInfrastructureProvisioner() {
    setInfrastructureProvisionerType(ARM.name());
  }

  @Override
  public String variableKey() {
    return VARIABLE_KEY;
  }
}
