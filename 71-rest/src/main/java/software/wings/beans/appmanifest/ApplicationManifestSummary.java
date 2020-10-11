package software.wings.beans.appmanifest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationManifestSummary {
  private String appManifestId;
  private String settingId;
  private ManifestSummary lastCollectedManifest;
  private ManifestSummary defaultManifest;
}
