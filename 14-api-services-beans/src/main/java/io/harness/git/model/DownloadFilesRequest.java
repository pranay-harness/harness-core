package io.harness.git.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DownloadFilesRequest extends FetchFilesByPathRequest {
  private String destinationDirectory;

  @Builder(builderMethodName = "downloadFilesRequestBuilder")
  DownloadFilesRequest(String repoUrl, String branch, String commitId, AuthRequest authRequest, String connectorId,
      String accountId, String repoType, List<String> filePaths, List<String> fileExtensions, boolean recursive,
      String destinationDirectory) {
    super(
        repoUrl, branch, commitId, authRequest, connectorId, accountId, repoType, filePaths, fileExtensions, recursive);
    this.destinationDirectory = destinationDirectory;
  }
}
