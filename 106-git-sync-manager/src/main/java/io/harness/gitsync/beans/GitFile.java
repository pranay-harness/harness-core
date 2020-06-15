package io.harness.gitsync.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitFile {
  private String filePath;
  private String fileContent;
}
