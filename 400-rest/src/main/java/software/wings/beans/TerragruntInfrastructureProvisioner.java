/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.InfrastructureProvisionerType.TERRAGRUNT;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDP)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("TERRAGRUNT")
@TargetModule(HarnessModule._957_CG_BEANS)
public class TerragruntInfrastructureProvisioner extends InfrastructureProvisioner implements TerraGroupProvisioners {
  public static final String VARIABLE_KEY = "terragrunt";
  @NotEmpty private String sourceRepoSettingId;
  private String sourceRepoBranch;
  private String commitId;
  @Trimmed(message = "repoName should not contain leading and trailing spaces") @Nullable private String repoName;
  @NotNull private String path;
  private String normalizedPath;

  private boolean templatized;
  private String secretManagerId;

  /**
   * Boolean to indicate if we should skip updating terraform state using refresh command before applying an approved
   * terraform plan
   */
  private boolean skipRefreshBeforeApplyingPlan;

  @Override
  public String variableKey() {
    return VARIABLE_KEY;
  }

  TerragruntInfrastructureProvisioner() {
    setInfrastructureProvisionerType(TERRAGRUNT.name());
  }

  @Builder
  private TerragruntInfrastructureProvisioner(String uuid, String appId, String name, String sourceRepoSettingId,
      String sourceRepoBranch, String commitId, String path, List<NameValuePair> variables,
      List<InfrastructureMappingBlueprint> mappingBlueprints, String accountId, String description,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath,
      String repoName, boolean skipRefreshBeforeApplyingPlan, String secretManagerId) {
    super(name, description, TERRAGRUNT.name(), variables, mappingBlueprints, accountId, uuid, appId, createdBy,
        createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    setSourceRepoSettingId(sourceRepoSettingId);
    setSourceRepoBranch(sourceRepoBranch);
    setCommitId(commitId);
    setPath(path);
    setNormalizedPath(FilenameUtils.normalize(path));
    setRepoName(repoName);
    setSecretManagerId(secretManagerId);
    this.skipRefreshBeforeApplyingPlan = skipRefreshBeforeApplyingPlan;
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
    private String commitId;
    private String path;
    private String normalizedPath;
    private String repoName;
    private String secretMangerName;
    private boolean skipRefreshBeforeApplyingPlan;

    @Builder
    public Yaml(String type, String harnessApiVersion, String description, String infrastructureProvisionerType,
        List<NameValuePair.Yaml> variables, List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints,
        String sourceRepoSettingName, String sourceRepoBranch, String path, String repoName, String commitId,
        boolean skipRefreshBeforeApplyingPlan, String secretMangerName) {
      super(type, harnessApiVersion, description, infrastructureProvisionerType, variables, mappingBlueprints);
      this.sourceRepoSettingName = sourceRepoSettingName;
      this.sourceRepoBranch = sourceRepoBranch;
      this.commitId = commitId;
      this.path = path;
      this.normalizedPath = FilenameUtils.normalize(path);
      this.repoName = repoName;
      this.secretMangerName = secretMangerName;
      this.skipRefreshBeforeApplyingPlan = skipRefreshBeforeApplyingPlan;
    }
  }
}
