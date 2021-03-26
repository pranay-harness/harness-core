package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.GCS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
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
