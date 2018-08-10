package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitFile {
  private String filePath;
  private String fileContent;
}
