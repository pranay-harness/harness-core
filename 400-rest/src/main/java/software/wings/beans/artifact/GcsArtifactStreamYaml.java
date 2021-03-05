package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.GCS;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class GcsArtifactStreamYaml extends ArtifactStreamYaml {
  private String bucketName;
  private List<String> artifactPaths;
  private String projectId;

  @lombok.Builder
  public GcsArtifactStreamYaml(
      String harnessApiVersion, String serverName, String bucketName, List<String> artifactPaths, String projectId) {
    super(GCS.name(), harnessApiVersion, serverName);
    this.bucketName = bucketName;
    this.artifactPaths = artifactPaths;
    this.projectId = projectId;
  }
}
