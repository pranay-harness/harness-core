package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.ECR;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EcrArtifactStreamYaml extends ArtifactStreamYaml {
  private String imageName;
  private String region;

  @lombok.Builder
  public EcrArtifactStreamYaml(String harnessApiVersion, String serverName, String imageName, String region) {
    super(ECR.name(), harnessApiVersion, serverName);
    this.imageName = imageName;
    this.region = region;
  }
}
