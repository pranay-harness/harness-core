package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AmazonS3ArtifactStreamYaml extends ArtifactStreamYaml {
  private String bucketName;
  private List<String> artifactPaths;

  @lombok.Builder
  public AmazonS3ArtifactStreamYaml(
      String harnessApiVersion, String serverName, String bucketName, List<String> artifactPaths) {
    super(AMAZON_S3.name(), harnessApiVersion, serverName);
    this.bucketName = bucketName;
    this.artifactPaths = artifactPaths;
  }
}
