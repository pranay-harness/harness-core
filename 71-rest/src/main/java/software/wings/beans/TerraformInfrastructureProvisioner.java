package software.wings.beans;

import static software.wings.beans.InfrastructureProvisionerType.TERRAFORM;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("TERRAFORM")
public class TerraformInfrastructureProvisioner extends InfrastructureProvisioner {
  public static final String VARIABLE_KEY = "terraform";
  @NotEmpty private String sourceRepoSettingId;

  /**
   * This could be either a branch or a commit id or any other reference which
   * can be checked out.
   */
  @NotEmpty private String sourceRepoBranch;
  @Trimmed(message = "repoName should not contain leading and trailing spaces") @Nullable private String repoName;
  @NotNull private String path;
  private String normalizedPath;
  private List<NameValuePair> backendConfigs;
  private List<NameValuePair> environmentVariables;
  private boolean templatized;
  private List<String> workspaces;

  @Override
  public String variableKey() {
    return VARIABLE_KEY;
  }

  TerraformInfrastructureProvisioner() {
    setInfrastructureProvisionerType(TERRAFORM.name());
  }

  @Builder
  private TerraformInfrastructureProvisioner(String uuid, String appId, String name, String sourceRepoSettingId,
      String sourceRepoBranch, String path, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints, String accountId, String description,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath,
      List<NameValuePair> backendConfigs, String repoName, List<NameValuePair> environmentVariables) {
    super(name, description, TERRAFORM.name(), variables, mappingBlueprints, accountId, uuid, appId, createdBy,
        createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    setSourceRepoSettingId(sourceRepoSettingId);
    setSourceRepoBranch(sourceRepoBranch);
    setPath(path);
    setNormalizedPath(FilenameUtils.normalize(path));
    this.backendConfigs = backendConfigs;
    setRepoName(repoName);
    this.environmentVariables = environmentVariables;
  }

  /**
   * The type Yaml.
   */
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class Yaml extends InfraProvisionerYaml {
    private String sourceRepoSettingName;
    private String sourceRepoBranch;
    private String path;
    private String normalizedPath;
    private List<NameValuePair.Yaml> backendConfigs;
    private List<NameValuePair.Yaml> environmentVariables;
    private String repoName;

    @Builder
    public Yaml(String type, String harnessApiVersion, String description, String infrastructureProvisionerType,
        List<NameValuePair.Yaml> variables, List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints,
        String sourceRepoSettingName, String sourceRepoBranch, String path, List<NameValuePair.Yaml> backendConfigs,
        String repoName, List<NameValuePair.Yaml> environmentVariables) {
      super(type, harnessApiVersion, description, infrastructureProvisionerType, variables, mappingBlueprints);
      this.sourceRepoSettingName = sourceRepoSettingName;
      this.sourceRepoBranch = sourceRepoBranch;
      this.path = path;
      this.normalizedPath = FilenameUtils.normalize(path);
      this.backendConfigs = backendConfigs;
      this.repoName = repoName;
      this.environmentVariables = environmentVariables;
    }
  }
}
