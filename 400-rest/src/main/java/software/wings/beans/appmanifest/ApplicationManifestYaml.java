package software.wings.beans.appmanifest;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.manifest.CustomSourceConfig;

import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.HelmCommandFlagConfig;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.yaml.BaseEntityYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class ApplicationManifestYaml extends BaseEntityYaml {
  private String storeType;
  private GitFileConfig gitFileConfig;
  private HelmChartConfig helmChartConfig;
  private KustomizeConfig kustomizeConfig;
  private CustomSourceConfig customSourceConfig;
  private Boolean pollForChanges;
  private Boolean skipVersioningForAllK8sObjects;
  private HelmCommandFlagConfig helmCommandFlag;

  @Builder
  public ApplicationManifestYaml(String type, String harnessApiVersion, String storeType, GitFileConfig gitFileConfig,
      HelmChartConfig helmChartConfig, KustomizeConfig kustomizeConfig, CustomSourceConfig customSourceConfig,
      Boolean pollForChanges, HelmCommandFlagConfig helmCommandFlag, Boolean skipVersioningForAllK8sObjects) {
    super(type, harnessApiVersion);
    this.storeType = storeType;
    this.gitFileConfig = gitFileConfig;
    this.helmChartConfig = helmChartConfig;
    this.kustomizeConfig = kustomizeConfig;
    this.customSourceConfig = customSourceConfig;
    this.pollForChanges = pollForChanges;
    this.skipVersioningForAllK8sObjects = skipVersioningForAllK8sObjects;
    this.helmCommandFlag = helmCommandFlag;
  }
}
