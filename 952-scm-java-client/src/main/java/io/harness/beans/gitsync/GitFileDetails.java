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
  String fileContent; // not needed in case of delete.
  String commitMessage;
  String oldFileSha; // not only in case of create file.
  String userEmail;
  String userName;
}
