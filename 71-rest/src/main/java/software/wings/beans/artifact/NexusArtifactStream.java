package software.wings.beans.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.utils.RepositoryType;

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
  private String repositoryType;
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
      String imageName, List<String> artifactPaths, String dockerPort, String dockerRegistryUrl,
      String repositoryType) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, NEXUS.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly);
    this.jobname = jobname;
    this.groupId = groupId;
    this.imageName = imageName;
    this.artifactPaths = artifactPaths;
    this.dockerPort = dockerPort;
    this.dockerRegistryUrl = dockerRegistryUrl;
    this.repositoryType = repositoryType;
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

  // TODO: remove this method after migration of old artifact streams
  // TODO: add validations for repository type
  public String getRepositoryType() {
    if (repositoryType != null) {
      return repositoryType;
    }
    if (isEmpty(artifactPaths)) {
      repositoryType = RepositoryType.docker.name();
    } else {
      repositoryType = RepositoryType.maven.name();
    }
    return repositoryType;
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
        .repositoryType(getRepositoryType())
        .build();
  }

  @Override
  public void validateRequiredFields() {
    if (appId.equals(GLOBAL_APP_ID)) {
      if (isEmpty(repositoryType)) {
        throw new InvalidRequestException("Repository Type cannot be empty", USER);
      }
    }
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
    private String repositoryType;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String repositoryName,
        String groupId, List<String> artifactPaths, String imageName, String dockerRegistryUrl, String repositoryType) {
      super(NEXUS.name(), harnessApiVersion, serverName, metadataOnly);
      this.repositoryName = repositoryName;
      this.groupId = groupId;
      this.artifactPaths = artifactPaths;
      this.imageName = imageName;
      this.dockerRegistryUrl = dockerRegistryUrl;
      this.repositoryType = repositoryType;
    }
  }
}
