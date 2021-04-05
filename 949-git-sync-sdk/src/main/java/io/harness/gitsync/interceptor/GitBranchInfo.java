package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitBranchInfoKeys")
@OwnedBy(DX)
public class GitBranchInfo {
  String branch;
  String yamlGitConfigId;
  String filePath;
  String accountId;
  String commitMsg;
  String lastCommitId;
}
