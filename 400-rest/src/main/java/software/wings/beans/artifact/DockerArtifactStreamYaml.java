package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
