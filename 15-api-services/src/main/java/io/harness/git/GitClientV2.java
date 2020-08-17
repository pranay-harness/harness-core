package io.harness.git;

import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.DiffRequest;
import io.harness.git.model.DiffResult;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.FetchFilesBwCommitsRequest;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitFile;

import java.util.List;

public interface GitClientV2 {
  void ensureRepoLocallyClonedAndUpdated(GitBaseRequest request);

  String validate(GitBaseRequest request);

  DiffResult diff(DiffRequest request);

  void commitAndPush(CommitAndPushRequest request);
  List<GitFile> fetchFilesByPath(FetchFilesByPathRequest request);
  void fetchFilesBetweenCommits(FetchFilesBwCommitsRequest request);
  void downloadFiles(DownloadFilesRequest request);
}
