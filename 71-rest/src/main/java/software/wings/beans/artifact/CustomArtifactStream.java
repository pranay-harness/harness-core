package software.wings.beans.artifact;

import static java.lang.String.format;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.template.artifacts.CustomRepositoryMapping;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@JsonTypeName("CUSTOM")
@Data
@EqualsAndHashCode(callSuper = false)
public class CustomArtifactStream extends ArtifactStream {
  public static final String ARTIFACT_SOURCE_NAME = "CUSTOM_ARTIFACT_STREAM";
  public static final String DEFAULT_SCRIPT_TIME_OUT = "60"; // 60 secs

  @NotNull private List<Script> scripts = new ArrayList<>();
  private List<String> tags = new ArrayList<>();

  @Data
  @Builder
  public static class Script {
    @Default @NotNull private Action action = Action.FETCH_VERSIONS;
    @NotEmpty private String scriptString;
    private String timeout;
    private CustomRepositoryMapping customRepositoryMapping;
  }

  public enum Action { FETCH_VERSIONS, DOWNLOAD_ARTIFACT, VALIDATE }

  public CustomArtifactStream() {
    super(ArtifactStreamType.CUSTOM.name());
    super.setMetadataOnly(true);
    super.setAutoPopulate(false);
  }

  @Builder
  public CustomArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, String serviceId, String scriptString, List<Script> scripts, List<String> tags) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath,
        ArtifactStreamType.CUSTOM.name(), sourceName, settingId, name, false, serviceId, true);
    this.scripts = scripts;
    this.tags = tags;
  }

  @Override
  public String generateSourceName() {
    return super.getName();
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    return anArtifactStreamAttributes().withArtifactStreamType(getArtifactStreamType()).build();
  }

  @Override
  public String fetchArtifactDisplayName(String buildNo) {
    return format("%s_%s", getName(), buildNo);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    @NotNull private List<Script> scripts = new ArrayList<>();
    private List<String> tags = new ArrayList<>();

    @lombok.Builder
    public Yaml(String harnessApiVersion, List<Script> scripts, List<String> tags) {
      super(CUSTOM.name(), harnessApiVersion);
      this.scripts = scripts;
      this.tags = tags;
    }
  }
}
