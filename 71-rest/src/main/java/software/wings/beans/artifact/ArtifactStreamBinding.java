package software.wings.beans.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

import java.util.List;

@Data
public class ArtifactStreamBinding {
  private String name;
  private List<String> artifactStreamIds;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYaml {
    private String name;
    private List<SingleBinding> bindings;

    @lombok.Builder
    public static final class SingleBinding {
      private String artifactServerName;
      private String artifactStreamName;
    }

    @lombok.Builder
    public Yaml(String name, List<SingleBinding> bindings) {
      this.name = name;
      this.bindings = bindings;
    }
  }
}
