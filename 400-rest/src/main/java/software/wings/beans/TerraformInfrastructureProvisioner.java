package software.wings.beans;

import static software.wings.beans.InfrastructureProvisionerType.TERRAFORM;

import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.validator.constraints.NotEmpty;

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
  private String sourceRepoBranch;
  private String commitId;
  @Trimmed(message = "repoName should not contain leading and trailing spaces") @Nullable private String repoName;
  @NotNull private String path;
  private String normalizedPath;
  private List<NameValuePair> backendConfigs;
  private List<NameValuePair> environmentVariables;
  private boolean templatized;
  private List<String> workspaces;
  private String kmsId;

  /**
   * Boolean to indicate if we should skip updating terraform state using refresh command before applying an approved
   * terraform plan
   */
  private boolean skipRefreshBeforeApplyingPlan;

  @Override
  public String variableKey() {
    return VARIABLE_KEY;
  }

  TerraformInfrastructureProvisioner() {
    setInfrastructureProvisionerType(TERRAFORM.name());
  }

  @Builder
  private TerraformInfrastructureProvisioner(String uuid, String appId, String name, String sourceRepoSettingId,
      String sourceRepoBranch, String commitId, String path, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints, String accountId, String description,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath,
      List<NameValuePair> backendConfigs, String repoName, List<NameValuePair> environmentVariables,
      boolean skipRefreshBeforeApplyingPlan, String kmsId) {
    super(name, description, TERRAFORM.name(), variables, mappingBlueprints, accountId, uuid, appId, createdBy,
        createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    setSourceRepoSettingId(sourceRepoSettingId);
    setSourceRepoBranch(sourceRepoBranch);
    setCommitId(commitId);
    setPath(path);
    setNormalizedPath(FilenameUtils.normalize(path));
    this.backendConfigs = backendConfigs;
    setRepoName(repoName);
    this.environmentVariables = environmentVariables;
    setKmsId(kmsId);
    this.skipRefreshBeforeApplyingPlan = skipRefreshBeforeApplyingPlan;
  }
}
