package software.wings.beans.appmanifest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestSummary {
  private String uuid;
  private String versionNo;
}
