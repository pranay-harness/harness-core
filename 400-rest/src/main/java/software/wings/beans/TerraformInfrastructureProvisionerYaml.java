package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FilenameUtils;

/**
 * The type Yaml.
 */
@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class TerraformInfrastructureProvisionerYaml extends InfraProvisionerYaml {
  private String sourceRepoSettingName;
  private String sourceRepoBranch;
  private String commitId;
  private String path;
  private String normalizedPath;
  private List<NameValuePairYaml> backendConfigs;
  private List<NameValuePairYaml> environmentVariables;
  private String repoName;
  private String secretMangerName;
  private boolean skipRefreshBeforeApplyingPlan;

  @Builder
  public TerraformInfrastructureProvisionerYaml(String type, String harnessApiVersion, String description,
      String infrastructureProvisionerType, List<NameValuePairYaml> variables,
      List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints, String sourceRepoSettingName,
      String sourceRepoBranch, String path, List<NameValuePairYaml> backendConfigs, String repoName,
      List<NameValuePairYaml> environmentVariables, String commitId, boolean skipRefreshBeforeApplyingPlan,
      String secretMangerName) {
    super(type, harnessApiVersion, description, infrastructureProvisionerType, variables, mappingBlueprints);
    this.sourceRepoSettingName = sourceRepoSettingName;
    this.sourceRepoBranch = sourceRepoBranch;
    this.commitId = commitId;
    this.path = path;
    this.normalizedPath = FilenameUtils.normalize(path);
    this.backendConfigs = backendConfigs;
    this.repoName = repoName;
    this.environmentVariables = environmentVariables;
    this.secretMangerName = secretMangerName;
    this.skipRefreshBeforeApplyingPlan = skipRefreshBeforeApplyingPlan;
  }
}
