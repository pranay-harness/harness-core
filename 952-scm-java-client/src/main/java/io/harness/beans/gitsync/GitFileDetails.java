/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
