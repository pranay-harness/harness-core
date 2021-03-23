package software.wings.beans.container;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.DeploymentSpecificationYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class UserDataSpecificationYaml extends DeploymentSpecificationYaml {
  private String data;

  @Builder
  public UserDataSpecificationYaml(String type, String harnessApiVersion, String data) {
    super(type, harnessApiVersion);
    this.data = data;
  }
}
