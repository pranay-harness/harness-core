package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitEntityInfoKeys")
@OwnedBy(DX)
public class GitEntityInfo {
  String branch;
  String yamlGitConfigId;
  String filePath;
  String commitMsg;
  boolean createPr;
  String lastObjectId; // required in case of update file
}
