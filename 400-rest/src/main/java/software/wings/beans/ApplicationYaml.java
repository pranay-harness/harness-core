package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ApplicationYaml extends BaseEntityYaml {
  private String description;

  @lombok.Builder
  public ApplicationYaml(String type, String harnessApiVersion, String description) {
    super(type, harnessApiVersion);
    this.description = description;
  }
}
