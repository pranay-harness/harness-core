package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.SMB;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class SmbArtifactStreamYaml extends ArtifactStreamYaml {
  private List<String> artifactPaths;

  @lombok.Builder
  public SmbArtifactStreamYaml(String harnessApiVersion, String serverName, List<String> artifactPaths) {
    super(SMB.name(), harnessApiVersion, serverName);
    this.artifactPaths = artifactPaths;
  }
}
