package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.yaml.BaseYaml;

@Data
@Builder
public class StorageConfiguration {
  @Attributes(title = "Host Source Path") private String hostSourcePath;
  @Attributes(title = "Container Path") private String containerPath;
  @Attributes(title = "Options") private boolean readonly = false;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @Builder
  public static final class Yaml extends BaseYaml {
    private String hostSourcePath;
    private String containerPath;
    private boolean readonly = false;
  }
}