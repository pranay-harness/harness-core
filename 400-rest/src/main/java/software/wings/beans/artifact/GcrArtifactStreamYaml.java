package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GcrArtifactStreamYaml extends ArtifactStreamYaml {
  private String registryHostName;
  private String dockerImageName;

  @Builder
  public GcrArtifactStreamYaml(
      String harnessApiVersion, String serverName, String registryHostName, String dockerImageName) {
    super(GCR.name(), harnessApiVersion, serverName);
    this.registryHostName = registryHostName;
    this.dockerImageName = dockerImageName;
  }
}
