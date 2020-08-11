package io.harness.git.model;

import java.util.List;

public class DownloadFilesRequest extends FetchFilesByPathRequest {
  private String destinationDirectory;

  DownloadFilesRequest(String repoUrl, String branch, String commitId, AuthRequest authRequest, String connectorId,
      String accountId, String repoType, List<String> filePaths, List<String> fileExtensions, boolean recursive,
      String destinationDirectory) {
    super(
        repoUrl, branch, commitId, authRequest, connectorId, accountId, repoType, filePaths, fileExtensions, recursive);
    this.destinationDirectory = destinationDirectory;
  }
}
