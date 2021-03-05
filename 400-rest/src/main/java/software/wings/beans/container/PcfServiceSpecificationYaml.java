package software.wings.beans.container;

import software.wings.beans.DeploymentSpecificationYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
