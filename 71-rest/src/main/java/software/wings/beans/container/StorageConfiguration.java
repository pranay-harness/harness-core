package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import io.harness.yaml.BaseYaml;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
public class StorageConfiguration {
  @Attributes(title = "Host Source Path") private String hostSourcePath;
  @Attributes(title = "Container Path") private String containerPath;
  @Attributes(title = "Options") private boolean readonly;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYaml {
    private String hostSourcePath;
    private String containerPath;
    private boolean readonly;

    @Builder
    public Yaml(String hostSourcePath, String containerPath, boolean readonly) {
      this.hostSourcePath = hostSourcePath;
      this.containerPath = containerPath;
      this.readonly = readonly;
    }
  }
}
