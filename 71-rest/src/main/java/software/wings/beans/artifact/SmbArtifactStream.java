package software.wings.beans.artifact;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.SMB;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;

import java.util.Date;
import java.util.List;

@JsonTypeName("SMB")
@Data
@EqualsAndHashCode(callSuper = true)
public class SmbArtifactStream extends ArtifactStream {
  @NotEmpty private List<String> artifactPaths;

  public SmbArtifactStream() {
    super(SMB.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public SmbArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String sourceName,
      String settingId, String name, boolean autoPopulate, String serviceId, List<String> artifactPaths) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath, SMB.name(),
        sourceName, settingId, name, autoPopulate, serviceId, true);
    this.artifactPaths = artifactPaths;
  }

  @SuppressFBWarnings("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
  @Override
  public String getArtifactDisplayName(String buildNo) {
    return isBlank(getSourceName()) ? format("%s_%s_%s", getSourceName(), buildNo, dateFormat.format(new Date()))
                                    : format("%s_%s", buildNo, dateFormat.format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withArtifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Override
  public String generateSourceName() {
    return getArtifactPaths().stream().map(artifactPath -> artifactPath + "").collect(joining(""));
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
      super(SMB.name(), harnessApiVersion, serverName, metadataOnly);
      this.bucketName = bucketName;
      this.artifactPaths = artifactPaths;
    }
  }
}
