package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ScmGitRefTaskResponseData implements DelegateResponseData {
  GitRefType gitRefType;
  String branch;
  String repoUrl;
  byte[] listBranchesResponse;
  byte[] listCommitsResponse;
  byte[] listCommitsInPRResponse;
  byte[] compareCommitsResponse;
  byte[] findPRResponse;
  byte[] getLatestCommitResponse;
  byte[] findCommitResponse;
}
