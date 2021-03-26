package software.wings.beans.container;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class StorageConfigurationYaml extends BaseYaml {
  private String hostSourcePath;
  private String containerPath;
  private boolean readonly;

  @Builder
  public StorageConfigurationYaml(String hostSourcePath, String containerPath, boolean readonly) {
    this.hostSourcePath = hostSourcePath;
    this.containerPath = containerPath;
    this.readonly = readonly;
  }
}
