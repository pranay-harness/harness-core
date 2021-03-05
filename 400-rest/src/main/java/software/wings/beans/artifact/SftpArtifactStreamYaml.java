package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.SFTP;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class SftpArtifactStreamYaml extends ArtifactStreamYaml {
  private List<String> artifactPaths;

  @lombok.Builder
  public SftpArtifactStreamYaml(String harnessApiVersion, String serverName, List<String> artifactPaths) {
    super(SFTP.name(), harnessApiVersion, serverName);
    this.artifactPaths = artifactPaths;
  }
}
