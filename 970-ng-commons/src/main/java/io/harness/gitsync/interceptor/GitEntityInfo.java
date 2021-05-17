package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitEntityInfoKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DX)
public class GitEntityInfo {
  String branch;
  String yamlGitConfigId;
  String folderPath;
  String filePath;
  String commitMsg;
  boolean createPr;
  String targetBranch;
  String lastObjectId; // required in case of update file
  boolean isNewBranch;
  boolean isSyncFromGit;
  boolean findDefaultFromOtherBranches;
  String baseBranch;
}
