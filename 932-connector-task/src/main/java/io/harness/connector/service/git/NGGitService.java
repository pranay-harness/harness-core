package io.harness.connector.service.git;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.git.model.AuthRequest;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.FetchFilesResult;
import io.harness.shell.SshSessionConfig;

@OwnedBy(HarnessTeam.DX)
public interface NGGitService {
  void validate(GitConfig gitConfig, String accountId, SshSessionConfig sshSessionConfig);

  void validateOrThrow(GitConfig gitConfig, String accountId, SshSessionConfig sshSessionConfig);

  CommitAndPushResult commitAndPush(GitConfig gitConfig, CommitAndPushRequest commitAndPushRequest, String accountId,
      SshSessionConfig sshSessionConfig);

  FetchFilesResult fetchFilesByPath(GitStoreDelegateConfig gitStoreDelegateConfig, String accountId,
      SshSessionConfig sshSessionConfig, GitConfig gitConfig);

  void downloadFiles(GitStoreDelegateConfig gitStoreDelegateConfig, String manifestFilesDirectory, String accountId,
      SshSessionConfig sshSessionConfig, GitConfig gitConfig);

  AuthRequest getAuthRequest(GitConfig gitConfig, SshSessionConfig sshSessionConfig);
}
