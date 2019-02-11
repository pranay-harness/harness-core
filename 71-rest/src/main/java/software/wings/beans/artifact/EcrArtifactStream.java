package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.text.SimpleDateFormat;
import java.util.Date;

@JsonTypeName("ECR")
@Data
@EqualsAndHashCode(callSuper = false)
public class EcrArtifactStream extends ArtifactStream {
  @NotEmpty private String region;
  @NotEmpty private String imageName;

  public EcrArtifactStream() {
    super(ECR.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public EcrArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String region, String imageName) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, ECR.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly);
    this.region = region;
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
        .withRegion(region)
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
    private String region;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String imageName, String region) {
      super(ECR.name(), harnessApiVersion, serverName, metadataOnly);
      this.imageName = imageName;
      this.region = region;
    }
  }
}
