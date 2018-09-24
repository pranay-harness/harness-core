package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;

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

/**
 * @author rktummala on 8/4/17.
 */
@JsonTypeName("GCR")
@Data
@EqualsAndHashCode(callSuper = false)
public class GcrArtifactStream extends ArtifactStream {
  @NotEmpty private String registryHostName;
  @NotEmpty private String dockerImageName;

  /**
   * Instantiates a new Docker artifact stream.
   */
  public GcrArtifactStream() {
    super(GCR.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public GcrArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String sourceName,
      String settingId, String name, boolean autoPopulate, String serviceId, String registryHostName,
      String dockerImageName) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath, GCR.name(),
        sourceName, settingId, name, autoPopulate, serviceId, true);
    this.registryHostName = registryHostName;
    this.dockerImageName = dockerImageName;
  }

  @SuppressFBWarnings("STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE")
  @Override
  public String getArtifactDisplayName(String buildNo) {
    return format("%s_%s_%s", getDockerImageName(), buildNo, dateFormat.format(new Date()));
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withImageName(dockerImageName)
        .withRegistryHostName(registryHostName)
        .build();
  }

  @Override
  public String generateSourceName() {
    return getRegistryHostName() + '/' + getDockerImageName();
  }

  @Override
  public String fetchRepositoryName() {
    return dockerImageName;
  }

  @Override
  public String fetchRegistryUrl() {
    return registryHostName;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String registryHostName;
    private String dockerImageName;

    @Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, String registryHostName,
        String dockerImageName) {
      super(GCR.name(), harnessApiVersion, serverName, metadataOnly);
      this.registryHostName = registryHostName;
      this.dockerImageName = dockerImageName;
    }
  }
}
