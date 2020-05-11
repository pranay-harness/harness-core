package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

@OwnedBy(CDC)
@JsonTypeName("GCS")
@Data
@EqualsAndHashCode(callSuper = true)
public class GcsArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;
  @NotEmpty private List<String> artifactPaths;
  @NotEmpty private String projectId;

  public GcsArtifactStream() {
    super(GCS.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public GcsArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, String jobname, List<String> artifactPaths, String projectId,
      String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, GCS.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true, accountId, keywords, sample);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
    this.projectId = projectId;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return isBlank(getSourceName())
        ? format("%s_%s_%s", getSourceName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()))
        : format("%s_%s_%s", getJobname(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .jobName(jobname)
        .artifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Override
  public String generateSourceName() {
    return getArtifactPaths().stream().map(artifactPath -> '/' + artifactPath).collect(joining("", getJobname(), ""));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactStream.Yaml {
    private String bucketName;
    private List<String> artifactPaths;
    private String projectId;

    @lombok.Builder
    public Yaml(
        String harnessApiVersion, String serverName, String bucketName, List<String> artifactPaths, String projectId) {
      super(GCS.name(), harnessApiVersion, serverName);
      this.bucketName = bucketName;
      this.artifactPaths = artifactPaths;
      this.projectId = projectId;
    }
  }
}
