package software.wings.beans.artifact;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.artifact.ArtifactStreamType.SFTP;

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

@JsonTypeName("SFTP")
@Data
@EqualsAndHashCode(callSuper = true)
public class SftpArtifactStream extends ArtifactStream {
  @NotEmpty private List<String> artifactPaths;

  public SftpArtifactStream() {
    super(SFTP.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public SftpArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, List<String> artifactPaths) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, SFTP.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true);
    this.artifactPaths = artifactPaths;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return isBlank(getSourceName())
        ? format("%s_%s_%s", getSourceName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()))
        : format("%s_%s", buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .artifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Override
  public String generateSourceName() {
    return getArtifactPaths().stream().map(artifactPath -> artifactPath + "").collect(joining(""));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactStream.Yaml {
    private List<String> artifactPaths;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, List<String> artifactPaths) {
      super(SFTP.name(), harnessApiVersion, serverName);
      this.artifactPaths = artifactPaths;
    }
  }
}