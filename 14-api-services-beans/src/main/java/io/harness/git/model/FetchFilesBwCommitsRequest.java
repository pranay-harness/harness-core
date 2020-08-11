package io.harness.git.model;

public class FetchFilesBwCommitsRequest extends GitBaseRequest {
  private String newCommitId;
  private String oldCommitId;

  FetchFilesBwCommitsRequest(String repoUrl, String branch, String commitId, AuthRequest authRequest,
      String connectorId, String accountId, String repoType, String newCommitId, String oldCommitId) {
    super(repoUrl, branch, commitId, authRequest, connectorId, accountId, repoType);
    this.newCommitId = newCommitId;
    this.oldCommitId = oldCommitId;
  }
}
