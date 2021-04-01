package io.harness.beans.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitFileDetails {
  private String filePath;
  private String branch;
  private String fileContent;
  private String commitMessage;
  private String oldFileSha; // Needed only in case of update file.
  private String userEmail;
}
