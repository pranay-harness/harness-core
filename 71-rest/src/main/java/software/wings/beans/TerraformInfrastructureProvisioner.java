package software.wings.beans;

import static software.wings.beans.InfrastructureProvisionerType.TERRAFORM;

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
@JsonTypeName("TERRAFORM")
public class TerraformInfrastructureProvisioner extends InfrastructureProvisioner {
  @NotEmpty private String sourceRepoSettingId;

  /**
   * This could be either a branch or a commit id or any other reference which
   * can be checked out.
   */
  private String sourceRepoBranch;
  private String path;
  private List<NameValuePair> backendConfigs;

  @Override
  public String variableKey() {
    return "terraform";
  }

  TerraformInfrastructureProvisioner() {
    setInfrastructureProvisionerType(TERRAFORM.name());
  }

  @Builder
  private TerraformInfrastructureProvisioner(String uuid, String appId, String name, String sourceRepoSettingId,
      String sourceRepoBranch, String path, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints, String description, EmbeddedUser createdBy,
      long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath,
      List<NameValuePair> backendConfigs) {
    super(name, description, TERRAFORM.name(), variables, mappingBlueprints, uuid, appId, createdBy, createdAt,
        lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    setSourceRepoSettingId(sourceRepoSettingId);
    setSourceRepoBranch(sourceRepoBranch);
    setPath(path);
    this.backendConfigs = backendConfigs;
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
    private String sourceRepoBranch;
    private String path;
    private List<NameValuePair.Yaml> backendConfigs;

    @Builder
    public Yaml(String type, String harnessApiVersion, String name, String description,
        String infrastructureProvisionerType, List<NameValuePair.Yaml> variables,
        List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints, String sourceRepoSettingName,
        String sourceRepoBranch, String path, List<NameValuePair.Yaml> backendConfigs) {
      super(type, harnessApiVersion, name, description, infrastructureProvisionerType, variables, mappingBlueprints);
      this.sourceRepoSettingName = sourceRepoSettingName;
      this.sourceRepoBranch = sourceRepoBranch;
      this.path = path;
      this.backendConfigs = backendConfigs;
    }
  }
}
