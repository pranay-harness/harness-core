package software.wings.beans.container;

import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
