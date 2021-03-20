package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.artifact.ArtifactStreamType.ECR;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.ff.FeatureFlagService;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
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
      String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, ECR.name(), sourceName,
        settingId, name, autoPopulate, serviceId, metadataOnly, accountId, keywords, sample);
    this.region = region;
    this.imageName = imageName;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getImageName(), buildNo, new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes(FeatureFlagService featureFlagService) {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .region(region)
        .imageName(imageName)
        .dockerBasedDeployment(true)
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

  @Override
  public boolean shouldValidate() {
    return true;
  }

  @Override
  public boolean checkIfStreamParameterized() {
    return validateParameters(region, imageName);
  }
}
