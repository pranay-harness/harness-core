package io.harness.beans.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitFileDetails {
  String filePath;
  String branch;
  String fileContent;
  String commitMessage;
  String oldFileSha; // Needed only in case of update file.
  String userEmail;
  String userName;
}
