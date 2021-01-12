package software.wings.beans;

import static software.wings.beans.InfrastructureProvisionerType.ARM;

import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.azure.arm.ARMScopeType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("ARM")
public class ARMInfrastructureProvisioner extends InfrastructureProvisioner {
  private static String VARIABLE_KEY = "arm";
  private ARMSourceType sourceType;
  private ARMScopeType scopeType;
  @Trimmed(message = "Template Body should not contain leading and trailing spaces") private String templateBody;
  private GitFileConfig gitFileConfig;

  @Builder
  private ARMInfrastructureProvisioner(String name, String description, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints, String accountId, String uuid, String appId,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath,
      ARMSourceType sourceType, String templateBody, GitFileConfig gitFileConfig, ARMScopeType scopeType) {
    super(name, description, ARM.name(), variables, mappingBlueprints, accountId, uuid, appId, createdBy, createdAt,
        lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.sourceType = sourceType;
    this.templateBody = templateBody;
    this.gitFileConfig = gitFileConfig;
    this.scopeType = scopeType;
  }

  ARMInfrastructureProvisioner() {
    setInfrastructureProvisionerType(ARM.name());
  }

  @Override
  public String variableKey() {
    return VARIABLE_KEY;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class Yaml extends InfraProvisionerYaml {
    private ARMSourceType sourceType;
    private String templateBody;
    private GitFileConfig gitFileConfig;
    private ARMScopeType scopeType;

    @Builder
    public Yaml(String type, String harnessApiVersion, String description, List<NameValuePair.Yaml> variables,
        List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints, ARMSourceType sourceType, String templateBody,
        GitFileConfig gitFileConfig, ARMScopeType scopeType) {
      super(type, harnessApiVersion, description, ARM.name(), variables, mappingBlueprints);
      this.sourceType = sourceType;
      this.templateBody = templateBody;
      this.gitFileConfig = gitFileConfig;
      this.scopeType = scopeType;
    }
  }
}
