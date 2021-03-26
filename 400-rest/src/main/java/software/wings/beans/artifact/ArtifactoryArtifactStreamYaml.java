package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;

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
public final class ArtifactoryArtifactStreamYaml extends ArtifactStreamYaml {
  private String repositoryName;
  private String groupId;
  private String imageName;
  private List<String> artifactPaths;
  private String artifactPattern;
  private String repositoryType;
  private String dockerRepositoryServer;
  private boolean metadataOnly;
  private boolean useDockerFormat;

  @lombok.Builder
  public ArtifactoryArtifactStreamYaml(String harnessApiVersion, String serverName, boolean metadataOnly,
      String repositoryName, String imageName, List<String> artifactPaths, String artifactPattern,
      String repositoryType, boolean useDockerFormat) {
    super(ARTIFACTORY.name(), harnessApiVersion, serverName);
    this.repositoryName = repositoryName;
    this.imageName = imageName;
    this.artifactPaths = artifactPaths;
    this.artifactPattern = artifactPattern;
    this.repositoryType = repositoryType;
    this.metadataOnly = metadataOnly;
    this.useDockerFormat = useDockerFormat;
  }
}
