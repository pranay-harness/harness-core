package io.harness.git.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FetchFilesBwCommitsRequest extends GitBaseRequest {
  private String newCommitId;
  private String oldCommitId;

  @Builder(builderMethodName = "fetchFilesBwCommitsRequestBuilder")
  FetchFilesBwCommitsRequest(String repoUrl, String branch, String commitId, AuthRequest authRequest,
      String connectorId, String accountId, String repoType, String newCommitId, String oldCommitId) {
    super(repoUrl, branch, commitId, authRequest, connectorId, accountId, repoType);
    this.newCommitId = newCommitId;
    this.oldCommitId = oldCommitId;
  }
}
