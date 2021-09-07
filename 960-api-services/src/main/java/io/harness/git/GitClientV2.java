package io.harness.git;

import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.DiffRequest;
import io.harness.git.model.DiffResult;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.FetchFilesBwCommitsRequest;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitBaseRequest;

public interface GitClientV2 {
  void ensureRepoLocallyClonedAndUpdated(GitBaseRequest request);

  String validate(GitBaseRequest request);

  void validateOrThrow(GitBaseRequest request);

  FetchFilesResult fetchFilesByPath(FetchFilesByPathRequest request);

  DiffResult diff(DiffRequest request);

  CommitAndPushResult commitAndPush(CommitAndPushRequest request);

  FetchFilesResult fetchFilesBetweenCommits(FetchFilesBwCommitsRequest request);

  void downloadFiles(DownloadFilesRequest request);
}
