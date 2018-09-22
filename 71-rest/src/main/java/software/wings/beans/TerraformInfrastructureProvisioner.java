package software.wings.beans;

import static software.wings.beans.InfrastructureProvisionerType.TERRAFORM;

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
@JsonTypeName("TERRAFORM")
public class TerraformInfrastructureProvisioner extends InfrastructureProvisioner {
  @NotEmpty private String sourceRepoSettingId;
  private String path;

  @Override
  public String variableKey() {
    return "terraform";
  }

  TerraformInfrastructureProvisioner() {
    setInfrastructureProvisionerType(TERRAFORM.name());
  }

  @Builder
  private TerraformInfrastructureProvisioner(String uuid, String appId, String name, String sourceRepoSettingId,
      String path, List<NameValuePair> variables, List<InfrastructureMappingBlueprint> mappingBlueprints,
      String description, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      List<String> keywords, String entityYamlPath) {
    super(name, description, TERRAFORM.name(), variables, mappingBlueprints, uuid, appId, createdBy, createdAt,
        lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    setSourceRepoSettingId(sourceRepoSettingId);
    setPath(path);
  }

  /**
   * The type Yaml.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @NoArgsConstructor
  public static final class Yaml extends InfrastructureProvisioner.Yaml {
    private String sourceRepoSettingName;
    private String path;

    @Builder
    public Yaml(String type, String harnessApiVersion, String name, String description,
        String infrastructureProvisionerType, List<NameValuePair.Yaml> variables,
        List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints, String sourceRepoSettingName, String path) {
      super(type, harnessApiVersion, name, description, infrastructureProvisionerType, variables, mappingBlueprints);
      this.sourceRepoSettingName = sourceRepoSettingName;
      this.path = path;
    }
  }
}
