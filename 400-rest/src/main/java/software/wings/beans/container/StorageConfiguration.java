package software.wings.beans.container;

import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageConfiguration {
  @Attributes(title = "Host Source Path") private String hostSourcePath;
  @Attributes(title = "Container Path") private String containerPath;
  @Attributes(title = "Options") private boolean readonly;
}
