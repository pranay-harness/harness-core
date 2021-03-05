package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JenkinsArtifactStreamYaml extends ArtifactStreamYaml {
  private String jobName;
  private List<String> artifactPaths;
  private boolean metadataOnly;

  @lombok.Builder
  public JenkinsArtifactStreamYaml(
      String harnessApiVersion, String serverName, boolean metadataOnly, String jobName, List<String> artifactPaths) {
    super(JENKINS.name(), harnessApiVersion, serverName);
    this.jobName = jobName;
    this.artifactPaths = artifactPaths;
    this.metadataOnly = metadataOnly;
  }
}
