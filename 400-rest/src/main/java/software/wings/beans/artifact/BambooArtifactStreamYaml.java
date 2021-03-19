package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;

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
public class BambooArtifactStreamYaml extends ArtifactStreamYaml {
  private String planName;
  private List<String> artifactPaths;
  private boolean metadataOnly;

  @lombok.Builder
  public BambooArtifactStreamYaml(
      String harnessApiVersion, String serverName, String planName, List<String> artifactPaths, boolean metadataOnly) {
    super(BAMBOO.name(), harnessApiVersion, serverName);
    this.planName = planName;
    this.artifactPaths = artifactPaths;
    this.metadataOnly = metadataOnly;
  }
}
