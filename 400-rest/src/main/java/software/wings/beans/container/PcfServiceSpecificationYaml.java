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
public final class PcfServiceSpecificationYaml extends DeploymentSpecificationYaml {
  private String maniefstYaml;
  private String serviceName;

  @Builder
  public PcfServiceSpecificationYaml(String type, String harnessApiVersion, String serviceName, String manifestYaml) {
    super(type, harnessApiVersion);
    this.maniefstYaml = manifestYaml;
    this.serviceName = serviceName;
  }
}
