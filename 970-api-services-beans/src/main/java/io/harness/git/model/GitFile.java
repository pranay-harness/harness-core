package io.harness.git.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitFile implements Serializable {
  private String filePath;
  private String fileContent;
}
