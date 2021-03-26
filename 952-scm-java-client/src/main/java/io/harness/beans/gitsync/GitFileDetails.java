package io.harness.beans.gitsync;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitFileDetails {
  private String filePath;
  private String branch;
  private String fileContent;
  private String commitMessage;
}
