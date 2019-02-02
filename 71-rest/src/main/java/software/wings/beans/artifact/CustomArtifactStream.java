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
  }

  public enum Action { FETCH_VERSIONS, DOWNLOAD_ARTIFACT, VALIDATE }

  public CustomArtifactStream() {
    super(ArtifactStreamType.CUSTOM.name());
    super.setMetadataOnly(true);
    super.setAutoPopulate(false);
  }

  @Builder
  public CustomArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, List<String> keywords, String entityYamlPath, String sourceName,
      String settingId, String name, String serviceId, String scriptString, List<Script> scripts, List<String> tags) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath,
        ArtifactStreamType.CUSTOM.name(), sourceName, settingId, name, false, serviceId, true);
    this.scripts = scripts;
    this.tags = tags;
  }

  @Override
  public String generateSourceName() {
    return super.getName();
  }

  @Override
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes().withArtifactStreamType(getArtifactStreamType()).build();
  }

  @Override
  public String getArtifactDisplayName(String buildNo) {
    return format("%s_%s", getName(), buildNo);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
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
