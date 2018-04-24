package software.wings.beans.artifact;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;

import java.util.Date;
import java.util.List;

@JsonTypeName("AMAZON_S3")
@Data
@EqualsAndHashCode(callSuper = true)
public class AmazonS3ArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;
  @NotEmpty private List<String> artifactPaths;

  public AmazonS3ArtifactStream() {
    super(AMAZON_S3.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public AmazonS3ArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String sourceName,
      String settingId, String name, boolean autoPopulate, String serviceId, String jobname,
      List<String> artifactPaths) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath, AMAZON_S3.name(),
        sourceName, settingId, name, autoPopulate, serviceId, true);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
  }

  @Override
  public String getArtifactDisplayName(String buildNo) {
    return isBlank(getSourceName()) ? String.format("%s_%s_%s", getSourceName(), buildNo, dateFormat.format(new Date()))
                                    : String.format("%s_%s_%s", getJobname(), buildNo, dateFormat.format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withJobName(jobname)
        .withArtifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Override
  public String generateSourceName() {
    return getArtifactPaths().stream().map(artifactPath -> '/' + artifactPath).collect(joining("", getJobname(), ""));
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactStream.Yaml {
    private String bucketName;
    private List<String> artifactPaths;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String bucketName,
        List<String> artifactPaths) {
      super(AMAZON_S3.name(), harnessApiVersion, serverName, metadataOnly);
      this.bucketName = bucketName;
      this.artifactPaths = artifactPaths;
    }
  }
}
