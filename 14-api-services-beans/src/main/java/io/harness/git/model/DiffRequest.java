package io.harness.git.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiffRequest extends GitBaseRequest {
  private String lastProcessedCommitId;
  private String endCommitId;
  private boolean excludeFilesOutsideSetupFolder;

  @Builder(builderMethodName = "diffRequestBuilder")
  public DiffRequest(String repoUrl, String branch, String commitId, AuthRequest authRequest, String connectorId,
      String accountId, String repoType, String lastProcessedCommitId, String endCommitId,
      boolean excludeFilesOutsideSetupFolder) {
    super(repoUrl, branch, commitId, authRequest, connectorId, accountId, repoType);
    this.lastProcessedCommitId = lastProcessedCommitId;
    this.endCommitId = endCommitId;
    this.excludeFilesOutsideSetupFolder = excludeFilesOutsideSetupFolder;
  }
}
