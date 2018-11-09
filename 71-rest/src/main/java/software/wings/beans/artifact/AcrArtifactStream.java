package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;

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

@JsonTypeName("ACR")
@Data
@EqualsAndHashCode(callSuper = true)
public class AcrArtifactStream extends ArtifactStream {
  @NotEmpty private String subscriptionId;
  @NotEmpty private String registryName;
  @NotEmpty private String repositoryName;

  public AcrArtifactStream() {
    super(ACR.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public AcrArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String sourceName,
      String settingId, String name, boolean autoPopulate, String serviceId, String subscriptionId, String registryName,
      String repositoryName) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath, ACR.name(),
        sourceName, settingId, name, autoPopulate, serviceId, true);
    this.subscriptionId = subscriptionId;
    this.registryName = registryName;
    this.repositoryName = repositoryName;
  }

  @SuppressFBWarnings("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
  @Override
  public String getArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getRegistryName() + "/" + getRepositoryName(), buildNo, dateFormat.format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withSubscriptionId(subscriptionId)
        .withRegistryName(registryName)
        .withRepositoryName(repositoryName)
        .build();
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
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String subscriptionId;
    private String registryName;
    private String repositoryName;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String subscriptionId,
        String registryName, String repositoryName) {
      super(ACR.name(), harnessApiVersion, serverName, metadataOnly);
      this.subscriptionId = subscriptionId;
      this.registryName = registryName;
      this.repositoryName = repositoryName;
    }
  }
}
