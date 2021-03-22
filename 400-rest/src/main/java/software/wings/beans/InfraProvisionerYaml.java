package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class InfraProvisionerYaml extends BaseEntityYaml {
  private String description;
  private String infrastructureProvisionerType;
  private List<NameValuePairYaml> variables;
  private List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints;

  public InfraProvisionerYaml(String type, String harnessApiVersion, String description,
      String infrastructureProvisionerType, List<NameValuePairYaml> variables,
      List<InfrastructureMappingBlueprint.Yaml> mappingBlueprints) {
    super(type, harnessApiVersion);
    this.description = description;
    this.infrastructureProvisionerType = infrastructureProvisionerType;
    this.mappingBlueprints = mappingBlueprints;
  }
}
