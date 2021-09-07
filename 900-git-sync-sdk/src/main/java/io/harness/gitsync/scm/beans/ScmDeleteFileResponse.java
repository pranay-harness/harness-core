package io.harness.gitsync.scm.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class ScmDeleteFileResponse implements ScmPushResponse {
  String folderPath;
  String filePath;
  boolean pushToDefaultBranch;
  String yamlGitConfigId;
  // Since file is deleted this will be set as old object Id
  String objectId;
  String projectIdentifier;
  String orgIdentifier;
  String accountIdentifier;
  String branch;
  String commitId;
}
