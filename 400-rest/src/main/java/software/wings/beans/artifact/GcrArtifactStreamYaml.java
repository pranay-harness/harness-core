package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.GCR;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
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
