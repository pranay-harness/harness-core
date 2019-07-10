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
import software.wings.utils.RepositoryFormat;
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
  private String packageName;
  private String repositoryFormat;

  public NexusArtifactStream() {
    super(NEXUS.name());
    if (getRepositoryFormat().equals(RepositoryFormat.docker.name())) {
      this.setMetadataOnly(true);
    }
  }

  @Builder
  public NexusArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String jobname, String groupId,
      String imageName, List<String> artifactPaths, String dockerPort, String dockerRegistryUrl, String repositoryType,
      String accountId, List<String> keywords, String packageName, String repositoryFormat) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, NEXUS.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly, accountId, keywords);
    this.jobname = jobname;
    this.groupId = groupId;
    this.imageName = imageName;
    this.artifactPaths = artifactPaths;
    this.dockerPort = dockerPort;
    this.dockerRegistryUrl = dockerRegistryUrl;
    this.repositoryType = repositoryType;
    this.packageName = packageName;
    this.repositoryFormat = repositoryFormat;
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
    if (getRepositoryFormat().equals(RepositoryFormat.docker.name())) {
      return format("%s_%s_%s", getJobname() + "/" + getImageName(), buildNo,
          new SimpleDateFormat(dateFormat).format(new Date()));
    } else if (getRepositoryFormat().equals(RepositoryFormat.nuget.name())
        || getRepositoryFormat().equals(RepositoryFormat.npm.name())) {
      return format("%s_%s_%s", getJobname() + "/" + getPackageName(), buildNo,
          new SimpleDateFormat(dateFormat).format(new Date()));
    }
    return null;
  }

  @Override
  public String generateSourceName() {
    StringBuilder builder = new StringBuilder(getJobname());
    if (isNotEmpty(artifactPaths)) {
      builder.append('/').append(getGroupId());
      getArtifactPaths().forEach(artifactPath -> builder.append('/').append(artifactPath));
    } else {
      if (getRepositoryFormat().equals(RepositoryFormat.docker.name())) {
        builder.append('/').append(getImageName());
      } else if (getRepositoryFormat().equals(RepositoryFormat.nuget.name())
          || getRepositoryFormat().equals(RepositoryFormat.npm.name())) {
        builder.append('/').append(getPackageName());
      }
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
      if (isEmpty(packageName)) {
        repositoryType = RepositoryType.docker.name();
      }
    } else {
      repositoryType = RepositoryType.maven.name();
    }
    return repositoryType;
  }

  public String getRepositoryFormat() {
    if (repositoryFormat != null) {
      return repositoryFormat;
    }
    if (isEmpty(artifactPaths)) {
      if (isEmpty(packageName)) {
        repositoryFormat = RepositoryFormat.docker.name();
      }
    } else {
      repositoryFormat = RepositoryFormat.maven.name();
    }
    return repositoryFormat;
  }

  @Override
  public boolean artifactSourceChanged(ArtifactStream artifactStream) {
    boolean changed = super.artifactSourceChanged(artifactStream);
    if (getRepositoryFormat().equals(RepositoryFormat.docker.name())) {
      return changed || registryUrlChanged(((NexusArtifactStream) artifactStream).dockerRegistryUrl);
    }
    return changed;
  }

  private boolean registryUrlChanged(String dockerRegistryUrl) {
    if (isEmpty(this.dockerRegistryUrl) && isEmpty(dockerRegistryUrl)) {
      return false;
    } else if ((isEmpty(this.dockerRegistryUrl) && isNotEmpty(dockerRegistryUrl))
        || (isNotEmpty(this.dockerRegistryUrl) && isEmpty(dockerRegistryUrl))) {
      return true;
    }
    return !this.dockerRegistryUrl.equals(dockerRegistryUrl);
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .jobName(jobname)
        .groupId(groupId)
        .repositoryName(jobname)
        .imageName(imageName)
        .artifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .nexusDockerPort(dockerPort)
        .nexusDockerRegistryUrl(dockerRegistryUrl)
        .repositoryType(getRepositoryType())
        .nexusPackageName(packageName)
        .repositoryFormat(getRepositoryFormat())
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
    private boolean metadataOnly;
    private String packageName;
    private String repositoryFormat;

    public void setRepositoryFormat(String repositoryFormat) {
      if (repositoryFormat != null) {
        this.repositoryFormat = repositoryFormat;
        return;
      }
      if (isEmpty(artifactPaths)) {
        if (isEmpty(packageName)) {
          this.repositoryFormat = RepositoryFormat.docker.name();
        }
      } else {
        this.repositoryFormat = RepositoryFormat.maven.name();
      }
    }

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String repositoryName,
        String groupId, List<String> artifactPaths, String imageName, String dockerRegistryUrl, String repositoryType,
        String packageName, String repositoryFormat) {
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
    }
  }
}
