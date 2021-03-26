package software.wings.beans.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.utils.RepositoryFormat;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NexusArtifactStreamYaml extends ArtifactStreamYaml {
  private String repositoryName;
  private String groupId;
  private List<String> artifactPaths;
  private String imageName;
  private String dockerRegistryUrl;
  private String repositoryType;
  private boolean metadataOnly;
  private String packageName;
  private String repositoryFormat;
  private String extension;
  private String classifier;

  public void setRepositoryFormat(String repositoryFormat) {
    if (repositoryFormat != null) {
      this.repositoryFormat = repositoryFormat;
      return;
    }
    if (isEmpty(getArtifactPaths())) {
      if (isEmpty(getPackageName())) {
        this.repositoryFormat = RepositoryFormat.docker.name();
      }
    } else {
      this.repositoryFormat = RepositoryFormat.maven.name();
    }
  }

  @lombok.Builder
  public NexusArtifactStreamYaml(String harnessApiVersion, String serverName, boolean metadataOnly,
      String repositoryName, String groupId, List<String> artifactPaths, String imageName, String dockerRegistryUrl,
      String repositoryType, String packageName, String repositoryFormat, String extension, String classifier) {
    super(NEXUS.name(), harnessApiVersion, serverName);
    this.repositoryName = repositoryName;
    this.groupId = groupId;
    this.artifactPaths = artifactPaths;
    this.imageName = imageName;
    this.dockerRegistryUrl = dockerRegistryUrl;
    this.repositoryType = repositoryType;
    this.metadataOnly = metadataOnly;
    this.packageName = packageName;
    this.repositoryFormat = repositoryFormat;
    this.extension = extension;
    this.classifier = classifier;
  }
}
