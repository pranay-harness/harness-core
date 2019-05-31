package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@JsonTypeName("BAMBOO")
@Data
@EqualsAndHashCode(callSuper = false)
public class BambooArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;
  @NotEmpty private List<String> artifactPaths;

  public BambooArtifactStream() {
    super(BAMBOO.name());
  }

  @Builder
  public BambooArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String jobname,
      List<String> artifactPaths, String accountId, List<String> keywords) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, BAMBOO.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly, accountId, keywords);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getSourceName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public String generateName() {
    return Utils.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    return getJobname();
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return ArtifactStreamAttributes.builder().artifactStreamType(getArtifactStreamType()).jobName(jobname).build();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String planName;
    private List<String> artifactPaths;
    private boolean metadataOnly;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, String planName, List<String> artifactPaths) {
      super(BAMBOO.name(), harnessApiVersion, serverName);
      this.planName = planName;
      this.artifactPaths = artifactPaths;
      this.metadataOnly = metadataOnly;
    }
  }
}
