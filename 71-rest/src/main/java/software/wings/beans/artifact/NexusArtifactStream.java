package software.wings.beans.artifact;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by srinivas on 3/31/17.
 */
@JsonTypeName("NEXUS")
@Data
@EqualsAndHashCode(callSuper = true)
public class NexusArtifactStream extends ArtifactStream {
  public static final String DOCKER_REGISTRY_URL_KEY = "dockerRegistryUrl";
  private String jobname;
  private String groupId;
  private String imageName;
  private List<String> artifactPaths;
  private String dockerPort;
  private String dockerRegistryUrl;

  public NexusArtifactStream() {
    super(NEXUS.name());
  }

  @Builder
  public NexusArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String jobname, String groupId,
      String imageName, List<String> artifactPaths, String dockerPort, String dockerRegistryUrl) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, NEXUS.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly);
    this.jobname = jobname;
    this.groupId = groupId;
    this.imageName = imageName;
    this.artifactPaths = artifactPaths;
    this.dockerPort = dockerPort;
    this.dockerRegistryUrl = dockerRegistryUrl;
  }

  // Do not remove this unless UI changes to start using groupId
  public void setGroupId(String groupId) {
    this.groupId = groupId;
    this.imageName = groupId;
  }
  public String fetchArtifactDisplayName(String buildNo) {
    if (isNotEmpty(artifactPaths)) {
      return format("%s_%s_%s", getSourceName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
    }
    return format(
        "%s_%s_%s", getJobname() + "/" + getImageName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
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

  @Override
  public String fetchRepositoryName() {
    return imageName;
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .jobName(jobname)
        .groupId(groupId)
        .imageName(imageName)
        .artifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .nexusDockerPort(dockerPort)
        .nexusDockerRegistryUrl(dockerRegistryUrl)
        .build();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String repositoryName;
    private String groupId;
    private List<String> artifactPaths;
    private String imageName;
    private String dockerRegistryUrl;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String repositoryName,
        String groupId, List<String> artifactPaths, String imageName, String dockerRegistryUrl) {
      super(NEXUS.name(), harnessApiVersion, serverName, metadataOnly);
      this.repositoryName = repositoryName;
      this.groupId = groupId;
      this.artifactPaths = artifactPaths;
      this.imageName = imageName;
      this.dockerRegistryUrl = dockerRegistryUrl;
    }
  }
}
