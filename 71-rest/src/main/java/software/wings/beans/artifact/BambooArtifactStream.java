package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamType.BAMBOO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@OwnedBy(CDC)
@JsonTypeName("BAMBOO")
@Data
@EqualsAndHashCode(callSuper = false)
public class BambooArtifactStream extends ArtifactStream {
  @NotEmpty private String jobname;
  private List<String> artifactPaths;

  public BambooArtifactStream() {
    super(BAMBOO.name());
  }

  @Builder
  public BambooArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String jobname,
      List<String> artifactPaths, String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, BAMBOO.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly, accountId, keywords, sample);
    this.jobname = jobname;
    this.artifactPaths = artifactPaths;
  }

  @Override
  public void validateRequiredFields() {
    if (!isMetadataOnly() && isEmpty(artifactPaths)) {
      throw new InvalidRequestException("Please provide at least one artifact path for non-metadata only");
    }
    // for both metadata and non-metadata remove artifact path containing empty strings
    List<String> updatedArtifactPathsList = new ArrayList<>();

    if (isNotEmpty(artifactPaths)) {
      for (String artifactPath : artifactPaths) {
        if (isNotEmpty(artifactPath.trim())) {
          updatedArtifactPathsList.add(artifactPath);
        }
      }
    }
    setArtifactPaths(updatedArtifactPathsList);
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
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .jobName(jobname)
        .artifactPaths(artifactPaths)
        .build();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String planName;
    private List<String> artifactPaths;
    private boolean metadataOnly;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, String planName, List<String> artifactPaths,
        boolean metadataOnly) {
      super(BAMBOO.name(), harnessApiVersion, serverName);
      this.planName = planName;
      this.artifactPaths = artifactPaths;
      this.metadataOnly = metadataOnly;
    }
  }
}
