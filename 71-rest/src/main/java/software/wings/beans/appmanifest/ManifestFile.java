package software.wings.beans.appmanifest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestFile {
  private String filePath;
  private String fileContent;
}
