package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The type Yaml.
 */
@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class CloudFormationInfrastructureProvisionerYaml extends InfraProvisionerYaml {
  private String sourceType;
  private String templateBody;
  private String templateFilePath;
  private GitFileConfig gitFileConfig;

  // TODO: check usage of yaml constructor
  @Builder
  public CloudFormationInfrastructureProvisionerYaml(String type, String harnessApiVersion, String description,
      String infrastructureProvisionerType, List<NameValuePair.Yaml> variables,
      List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints, String sourceType, String templateBody,
      String templateFilePath, GitFileConfig gitFileConfig) {
    super(type, harnessApiVersion, description, infrastructureProvisionerType, variables, mappingBlueprints);
    this.sourceType = sourceType;
    this.templateBody = templateBody;
    this.templateFilePath = templateFilePath;
    this.gitFileConfig = gitFileConfig;
  }
}
