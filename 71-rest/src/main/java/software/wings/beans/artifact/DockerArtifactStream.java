package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Date;
import java.util.List;

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
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String sourceName,
      String settingId, String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String imageName) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath, DOCKER.name(),
        sourceName, settingId, name, autoPopulate, serviceId, true);
    this.imageName = imageName;
  }

  @SuppressFBWarnings("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
  @Override
  public String getArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getImageName(), buildNo, dateFormat.format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
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
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String imageName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String imageName) {
      super(DOCKER.name(), harnessApiVersion, serverName, metadataOnly);
      this.imageName = imageName;
    }
  }
}
