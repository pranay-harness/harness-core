package software.wings.beans.appmanifest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestFile {
  private String fileName;
  private String fileContent;
}
