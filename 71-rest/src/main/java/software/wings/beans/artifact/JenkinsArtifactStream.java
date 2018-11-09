package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.JENKINS;

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
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String sourceName,
      String settingId, String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String jobname,
      List<String> artifactPaths) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath, JENKINS.name(),
        sourceName, settingId, name, autoPopulate, serviceId, metadataOnly);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
  }

  @Override
  public String generateSourceName() {
    return getJobname();
  }

  @SuppressFBWarnings("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
  @Override
  public String getArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getJobname(), buildNo, dateFormat.format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes().withArtifactStreamType(getArtifactStreamType()).withJobName(jobname).build();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
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
