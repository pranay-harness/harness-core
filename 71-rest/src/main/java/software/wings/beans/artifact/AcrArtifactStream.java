package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;

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
import java.util.Set;

@OwnedBy(CDC)
@JsonTypeName("ACR")
@Data
@EqualsAndHashCode(callSuper = true)
public class AcrArtifactStream extends ArtifactStream {
  @NotEmpty private String subscriptionId;
  @NotEmpty private String registryName;
  private String registryHostName;
  @NotEmpty private String repositoryName;

  public AcrArtifactStream() {
    super(ACR.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public AcrArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, String subscriptionId, String registryName,
      String registryHostName, String repositoryName, String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, ACR.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true, accountId, keywords, sample);
    this.subscriptionId = subscriptionId;
    this.registryName = registryName;
    this.registryHostName = registryHostName;
    this.repositoryName = repositoryName;
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getRegistryName() + "/" + getRepositoryName(), buildNo,
        new SimpleDateFormat(dateFormat).format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .subscriptionId(subscriptionId)
        .registryName(registryName)
        .registryHostName(registryHostName)
        .repositoryName(repositoryName)
        .build();
  }

  @Override
  public boolean shouldValidate() {
    return true;
  }

  @Override
  public String generateSourceName() {
    return getRegistryName() + '/' + getRepositoryName();
  }

  @Override
  public String fetchRepositoryName() {
    return getRepositoryName();
  }

  @Override
  public String fetchRegistryUrl() {
    return getRegistryName();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String subscriptionId;
    private String registryName;
    private String registryHostName;
    private String repositoryName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String subscriptionId,
        String registryName, String registryHostName, String repositoryName) {
      super(ACR.name(), harnessApiVersion, serverName);
      this.subscriptionId = subscriptionId;
      this.registryName = registryName;
      this.registryHostName = registryHostName;
      this.repositoryName = repositoryName;
    }
  }
}
