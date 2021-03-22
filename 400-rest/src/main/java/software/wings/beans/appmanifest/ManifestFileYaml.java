package software.wings.beans.appmanifest;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class ManifestFileYaml extends BaseEntityYaml {
  private String fileContent;

  @Builder
  public ManifestFileYaml(String type, String harnessApiVersion, String fileContent) {
    super(type, harnessApiVersion);
    this.fileContent = fileContent;
  }
}
