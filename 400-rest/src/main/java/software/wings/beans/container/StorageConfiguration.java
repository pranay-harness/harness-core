package software.wings.beans.container;

import io.harness.yaml.BaseYaml;

import com.github.reinert.jjschema.Attributes;
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
}
