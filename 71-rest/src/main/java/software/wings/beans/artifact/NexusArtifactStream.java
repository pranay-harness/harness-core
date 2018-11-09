package software.wings.beans.artifact;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Created by srinivas on 3/31/17.
 */
@JsonTypeName("NEXUS")
@Data
@EqualsAndHashCode(callSuper = true)
public class NexusArtifactStream extends ArtifactStream {
  private String jobname;
  private String groupId;
  private String imageName;
  private List<String> artifactPaths;
  private String dockerPort;

  public NexusArtifactStream() {
    super(NEXUS.name());
  }

  @Builder
  public NexusArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String sourceName,
      String settingId, String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String jobname,
      String groupId, String imageName, List<String> artifactPaths, String dockerPort) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath, NEXUS.name(),
        sourceName, settingId, name, autoPopulate, serviceId, metadataOnly);
    this.jobname = jobname;
    this.groupId = groupId;
    this.imageName = imageName;
    this.artifactPaths = artifactPaths;
    this.dockerPort = dockerPort;
  }

  @SuppressFBWarnings("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
  public String getArtifactDisplayName(String buildNo) {
    if (isNotEmpty(artifactPaths)) {
      return format("%s_%s_%s", getSourceName(), buildNo, dateFormat.format(new Date()));
    }
    return format("%s_%s_%s", getJobname() + "/" + getImageName(), buildNo, dateFormat.format(new Date()));
  }

  @Override
  public String generateSourceName() {
    StringBuilder builder = new StringBuilder(getJobname());
    if (isNotEmpty(artifactPaths)) {
      builder.append('/').append(getGroupId());
      getArtifactPaths().forEach(artifactPath -> builder.append('/').append(artifactPath));
    } else {
      builder.append('/').append(getImageName());
    }
    return builder.toString();
  }

  /**
   * Set Group Id
   */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
    this.imageName = groupId;
  }

  @Override
  public String fetchRepositoryName() {
    return imageName;
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withJobName(jobname)
        .withGroupId(groupId)
        .withImageName(imageName)
        .withArtifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String repositoryName;
    private String groupId;
    private List<String> artifactPaths;
    private String imageName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String repositoryName,
        String groupId, List<String> artifactPaths, String imageName) {
      super(NEXUS.name(), harnessApiVersion, serverName, metadataOnly);
      this.repositoryName = repositoryName;
      this.groupId = groupId;
      this.artifactPaths = artifactPaths;
      this.imageName = imageName;
    }
  }
}
