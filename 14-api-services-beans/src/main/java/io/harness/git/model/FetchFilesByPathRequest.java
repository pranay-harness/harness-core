package io.harness.git.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FetchFilesByPathRequest extends GitBaseRequest {
  private List<String> filePaths;
  private List<String> fileExtensions;
  private boolean recursive;

  @Builder(builderMethodName = "fetchFilesByPathRequestBuilder")
  FetchFilesByPathRequest(String repoUrl, String branch, String commitId, AuthRequest authRequest, String connectorId,
      String accountId, String repoType, List<String> filePaths, List<String> fileExtensions, boolean recursive) {
    super(repoUrl, branch, commitId, authRequest, connectorId, accountId, repoType);
    this.filePaths = filePaths;
    this.fileExtensions = fileExtensions;
    this.recursive = recursive;
  }
}
