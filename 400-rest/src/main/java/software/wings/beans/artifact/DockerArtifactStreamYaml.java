package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DockerArtifactStreamYaml extends ArtifactStreamYaml {
  private String imageName;

  @lombok.Builder
  public DockerArtifactStreamYaml(String harnessApiVersion, String serverName, String imageName) {
    super(DOCKER.name(), harnessApiVersion, serverName);
    this.imageName = imageName;
  }
}
