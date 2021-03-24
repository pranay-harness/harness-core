package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ServiceYaml extends BaseEntityYaml {
  private String description;
  private String artifactType;
  private String deploymentType;
  private String configMapYaml;
  private String applicationStack;
  private String helmVersion;
  private List<NameValuePairYaml> configVariables = new ArrayList<>();

  /*
   Support for Custom Deployment
    */
  private String deploymentTypeTemplateUri;
  private String deploymentTypeTemplateVersion;

  @lombok.Builder
  public ServiceYaml(String harnessApiVersion, String description, String artifactType, String deploymentType,
      String configMapYaml, String applicationStack, List<NameValuePairYaml> configVariables, String helmVersion,
      String deploymentTypeTemplateUri, String deploymentTypeTemplateVersion) {
    super(EntityType.SERVICE.name(), harnessApiVersion);
    this.description = description;
    this.artifactType = artifactType;
    this.deploymentType = deploymentType;
    this.configMapYaml = configMapYaml;
    this.applicationStack = applicationStack;
    this.configVariables = configVariables;
    this.helmVersion = helmVersion;
    this.deploymentTypeTemplateUri = deploymentTypeTemplateUri;
    this.deploymentTypeTemplateVersion = deploymentTypeTemplateVersion;
  }
}
