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
public class SCMNoOpResponse implements ScmPushResponse {
  String filePath;
  boolean pushToDefaultBranch;
  String yamlGitConfigId;
  String objectId;
  String folderPath;
  String branch;
  String commitId;
}
