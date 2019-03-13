package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;

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

@JsonTypeName("JENKINS")
@Data
@EqualsAndHashCode(callSuper = false)
public class JenkinsArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;
  @NotEmpty private List<String> artifactPaths;

  public JenkinsArtifactStream() {
    super(JENKINS.name());
  }

  @Builder
  public JenkinsArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String jobname,
      List<String> artifactPaths) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, JENKINS.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
  }

  @Override
  public String generateSourceName() {
    return getJobname();
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getJobname(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return ArtifactStreamAttributes.builder().artifactStreamType(getArtifactStreamType()).jobName(jobname).build();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String jobName;
    private List<String> artifactPaths;

    @lombok.Builder
    public Yaml(
        String harnessApiVersion, String serverName, boolean metadataOnly, String jobName, List<String> artifactPaths) {
      super(JENKINS.name(), harnessApiVersion, serverName, metadataOnly);
      this.jobName = jobName;
      this.artifactPaths = artifactPaths;
    }
  }
}
