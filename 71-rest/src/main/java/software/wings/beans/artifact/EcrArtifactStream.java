package software.wings.beans.artifact;

import static java.lang.String.format;
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
import java.util.List;

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
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String region, String imageName,
      String accountId, List<String> keywords) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, ECR.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly, accountId, keywords);
    this.region = region;
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
        .region(region)
        .imageName(imageName)
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
    private String region;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, String imageName, String region) {
      super(ECR.name(), harnessApiVersion, serverName);
      this.imageName = imageName;
      this.region = region;
    }
  }
}
