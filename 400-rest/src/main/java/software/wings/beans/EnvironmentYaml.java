package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class EnvironmentYaml extends BaseEntityYaml {
  private String description;
  private String configMapYaml;
  private Map<String, String> configMapYamlByServiceTemplateName;
  private String environmentType = "NON_PROD";
  private List<VariableOverrideYaml> variableOverrides = new ArrayList<>();

  @lombok.Builder
  public EnvironmentYaml(String harnessApiVersion, String description, String configMapYaml,
      Map<String, String> configMapYamlByServiceTemplateName, String environmentType,
      List<VariableOverrideYaml> variableOverrides) {
    super(EntityType.ENVIRONMENT.name(), harnessApiVersion);
    this.description = description;
    this.configMapYaml = configMapYaml;
    this.configMapYamlByServiceTemplateName = configMapYamlByServiceTemplateName;
    this.environmentType = environmentType;
    this.variableOverrides = variableOverrides;
  }
}
