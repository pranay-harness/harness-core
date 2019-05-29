package software.wings.beans.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

@Data
@EqualsAndHashCode(callSuper = false)
public class ArtifactStreamBinding {
  private String artifactStreamId;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYaml {
    private String artifactStreamType;
    private String artifactServerName;
    private String artifactStreamName;

    @lombok.Builder
    public Yaml(String artifactStreamType, String artifactServerName, String artifactStreamName) {
      this.artifactStreamType = artifactStreamType;
      this.artifactServerName = artifactServerName;
      this.artifactStreamName = artifactStreamName;
    }
  }
}
