package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 1/5/17.
 */
@OwnedBy(CDC)
@JsonTypeName("DOCKER")
@Data
@EqualsAndHashCode(callSuper = false)
public class DockerArtifactStream extends ArtifactStream {
  @NotEmpty private String imageName;

  public DockerArtifactStream() {
    super(DOCKER.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public DockerArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String imageName, String accountId,
      Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, DOCKER.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true, accountId, keywords, sample);
    this.imageName = imageName;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getImageName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .imageName(imageName)
        .dockerBasedDeployment(true)
        .build();
  }

  @Override
  public String generateSourceName() {
    return getImageName();
  }

  @Override
  public String fetchRepositoryName() {
    return imageName;
  }

  @Override
  public boolean shouldValidate() {
    return true;
  }

  @Override
  public boolean checkIfStreamParameterized() {
    return validateParameters(imageName);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStreamYaml {
    private String imageName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, String imageName) {
      super(DOCKER.name(), harnessApiVersion, serverName);
      this.imageName = imageName;
    }
  }
}
