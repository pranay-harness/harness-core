package software.wings.beans.artifact;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.utils.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@JsonTypeName("ARTIFACTORY")
@Data
@EqualsAndHashCode(callSuper = true)
public class ArtifactoryArtifactStream extends ArtifactStream {
  private String repositoryType = "any";
  @NotEmpty private String jobname;
  private String groupId;
  private String imageName;
  private List<String> artifactPaths;
  private String artifactPattern;
  private String dockerRepositoryServer;

  public ArtifactoryArtifactStream() {
    super(ARTIFACTORY.name());
  }

  @Builder
  public ArtifactoryArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String repositoryType, String jobname,
      String groupId, String imageName, List<String> artifactPaths, String artifactPattern,
      String dockerRepositoryServer) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, ARTIFACTORY.name(),
        sourceName, settingId, name, autoPopulate, serviceId, metadataOnly);
    this.repositoryType = repositoryType;
    this.jobname = jobname;
    this.groupId = groupId;
    this.imageName = imageName;
    this.artifactPaths = artifactPaths;
    this.artifactPattern = artifactPattern;
    this.dockerRepositoryServer = dockerRepositoryServer;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return isBlank(getImageName())
        ? format("%s_%s_%s", getSourceName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()))
        : format("%s_%s_%s", getJobname() + "/" + getImageName(), buildNo,
              new SimpleDateFormat(dateFormat).format(new Date()));
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
  public String generateName() {
    return Util.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    return getJobname() + '/' + (isBlank(getImageName()) ? getArtifactPattern() : getImageName());
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withJobName(jobname)
        .withImageName(imageName)
        .withGroupId(getGroupId())
        .withArtifactPattern(artifactPattern)
        .withArtifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .withRepositoryType(repositoryType)
        .withMetadataOnly(isMetadataOnly())
        .withArtifactoryDockerRepositoryServer(dockerRepositoryServer)
        .build();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactStream.Yaml {
    private String repositoryName;
    private String groupId;
    private String imageName;
    private List<String> artifactPaths;
    private String artifactPattern;
    private String repositoryType;
    private String dockerRepositoryServer;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String repositoryName,
        String groupId, String imageName, List<String> artifactPaths, String artifactPattern) {
      super(ARTIFACTORY.name(), harnessApiVersion, serverName, metadataOnly);
      this.repositoryName = repositoryName;
      this.groupId = groupId;
      this.imageName = imageName;
      this.artifactPaths = artifactPaths;
      this.artifactPattern = artifactPattern;
    }
  }
}
