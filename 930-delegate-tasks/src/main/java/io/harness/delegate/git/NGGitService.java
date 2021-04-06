package io.harness.delegate.git;

import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.git.model.AuthRequest;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.CommitAndPushResult;
import io.harness.git.model.FetchFilesResult;
import io.harness.shell.SshSessionConfig;

public interface NGGitService {
  String validate(GitConfigDTO gitConfig, String accountId, SshSessionConfig sshSessionConfig);

  CommitAndPushResult commitAndPush(GitConfigDTO gitConfig, CommitAndPushRequest commitAndPushRequest, String accountId,
      SshSessionConfig sshSessionConfig);

  FetchFilesResult fetchFilesByPath(GitStoreDelegateConfig gitStoreDelegateConfig, String accountId,
      SshSessionConfig sshSessionConfig, GitConfigDTO gitConfigDTO);

  void downloadFiles(GitStoreDelegateConfig gitStoreDelegateConfig, String manifestFilesDirectory, String accountId,
      SshSessionConfig sshSessionConfig, GitConfigDTO gitConfigDTO);

  AuthRequest getAuthRequest(GitConfigDTO gitConfig, SshSessionConfig sshSessionConfig);
}
