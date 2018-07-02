package software.wings.sm;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DelegateMetaInfo {
  private String id;
  private String hostName;
  private String version;
}