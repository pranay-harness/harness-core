package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by anubhaw on 1/5/17.
 */
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
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String imageName) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, DOCKER.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true);
    this.imageName = imageName;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getImageName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withImageName(imageName)
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

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String imageName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String imageName) {
      super(DOCKER.name(), harnessApiVersion, serverName, metadataOnly);
      this.imageName = imageName;
    }
  }
}
