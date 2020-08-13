package io.harness.git;

import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.DiffRequest;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.FetchFilesBwCommitsRequest;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.GitBaseRequest;

public interface GitClientV2 {
  void ensureRepoLocallyClonedAndUpdated(GitBaseRequest request);
  String validate(GitBaseRequest request);
  void diff(DiffRequest request);
  void commitAndPush(CommitAndPushRequest request);
  void fetchFilesByPath(FetchFilesByPathRequest request);
  void fetchFilesBetweenCommits(FetchFilesBwCommitsRequest request);
  void downloadFiles(DownloadFilesRequest request);
}
