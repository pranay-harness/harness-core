package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class GitFile implements Serializable {
  private String filePath;
  private String fileContent;
}
