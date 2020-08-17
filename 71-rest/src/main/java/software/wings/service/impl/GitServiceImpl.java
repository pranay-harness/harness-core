package software.wings.service.impl;

import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.KERBEROS;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.SshHelperUtils.createSshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.harness.exception.InvalidRequestException;
import io.harness.git.GitClientV2;
import io.harness.git.UsernamePasswordAuthRequest;
import io.harness.git.model.AuthRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.JgitSshAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.GitFileConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.NoopExecutionCallback;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFilesBetweenCommitsRequest;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.service.impl.yaml.GitClientImpl;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.utils.SshHelperUtils;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class GitServiceImpl implements GitService {
  @Inject private GitClient gitClient;
  @Inject private GitClientV2 gitClientV2;

  private String getSafeRepoType(GitRepositoryType repositoryType) {
    return null != repositoryType ? repositoryType.name() : null;
  }

  @Override
  public String validate(GitConfig gitConfig) {
    AuthRequest authRequest = getAuthRequest(gitConfig);
    return gitClientV2.validate(GitBaseRequest.builder()
                                    .branch(gitConfig.getBranch())
                                    .repoUrl(gitConfig.getRepoUrl())
                                    .repoType(getSafeRepoType(gitConfig.getGitRepoType()))
                                    .authRequest(authRequest)
                                    .build());
  }

  @Override
  public void ensureRepoLocallyClonedAndUpdated(GitOperationContext gitOperationContext) {
    GitConfig gitConfig = gitOperationContext.getGitConfig();
    AuthRequest authRequest = getAuthRequest(gitConfig);
    gitClientV2.ensureRepoLocallyClonedAndUpdated(GitBaseRequest.builder()
                                                      .branch(gitConfig.getBranch())
                                                      .commitId(gitConfig.getReference())
                                                      .repoUrl(gitConfig.getRepoUrl())
                                                      .repoType(getSafeRepoType(gitConfig.getGitRepoType()))
                                                      .authRequest(authRequest)
                                                      .accountId(gitConfig.getAccountId())
                                                      .connectorId(gitOperationContext.getGitConnectorId())
                                                      .build());
  }

  private AuthRequest getAuthRequest(GitConfig gitConfig) {
    AuthRequest authRequest;
    if (gitConfig.getRepoUrl().toLowerCase().startsWith("http")) {
      String username = gitConfig.getUsername();
      char[] password = gitConfig.getPassword();
      if (KERBEROS == gitConfig.getAuthenticationScheme()) {
        addApacheConnectionFactoryAndGenerateTGT(gitConfig);
        username = ((HostConnectionAttributes) gitConfig.getSshSettingAttribute().getValue())
                       .getKerberosConfig()
                       .getPrincipal(); // set principal as username
      }
      authRequest = UsernamePasswordAuthRequest.builder().username(username).password(password).build();
    } else {
      authRequest =
          JgitSshAuthRequest.builder().factory(getSshSessionFactory(gitConfig.getSshSettingAttribute())).build();
    }
    return authRequest;
  }

  private SshSessionFactory getSshSessionFactory(SettingAttribute settingAttribute) {
    return new JschConfigSessionFactory() {
      @Override
      protected Session createSession(OpenSshConfig.Host hc, String user, String host, int port, FS fs)
          throws JSchException {
        SshSessionConfig sshSessionConfig = createSshSessionConfig(settingAttribute, host);
        sshSessionConfig.setPort(port); // use port from repo URL
        return getSSHSession(sshSessionConfig);
      }

      @Override
      protected void configure(OpenSshConfig.Host hc, Session session) {}
    };
  }

  private void addApacheConnectionFactoryAndGenerateTGT(GitConfig gitConfig) {
    try {
      HttpTransport.setConnectionFactory(new GitClientImpl.ApacheHttpConnectionFactory());
      URL url = new URL(gitConfig.getRepoUrl());
      SshSessionConfig sshSessionConfig = createSshSessionConfig(gitConfig.getSshSettingAttribute(), url.getHost());
      SshHelperUtils.generateTGT(sshSessionConfig.getUserName(),
          sshSessionConfig.getPassword() != null ? new String(sshSessionConfig.getPassword()) : null,
          sshSessionConfig.getKeyPath(), new NoopExecutionCallback());
    } catch (Exception e) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception while setting kerberos auth for repo: [{}] with ex: [{}]",
          gitConfig.getRepoUrl(), getMessage(e));
      throw new InvalidRequestException("Failed to do Kerberos authentication");
    }
  }

  @Override
  public GitFetchFilesResult fetchFilesByPath(GitConfig gitConfig, String connectorId, String commitId, String branch,
      List<String> filePaths, boolean useBranch) {
    return gitClient.fetchFilesByPath(gitConfig,
        GitFetchFilesRequest.builder()
            .commitId(commitId)
            .branch(branch)
            .filePaths(filePaths)
            .gitConnectorId(connectorId)
            .useBranch(useBranch)
            .recursive(true)
            .build());
  }

  @Override
  public void downloadFiles(GitConfig gitConfig, GitFileConfig gitFileConfig, String destinationDirectory) {
    gitClient.downloadFiles(gitConfig,
        GitFetchFilesRequest.builder()
            .commitId(gitFileConfig.getCommitId())
            .branch(gitFileConfig.getBranch())
            .filePaths(Collections.singletonList(gitFileConfig.getFilePath()))
            .gitConnectorId(gitFileConfig.getConnectorId())
            .useBranch(gitFileConfig.isUseBranch())
            .recursive(true)
            .build(),
        destinationDirectory);
  }

  @Override
  public GitFetchFilesResult fetchFilesBetweenCommits(
      GitConfig gitConfig, String newCommitId, String oldCommitId, String connectorId) {
    return gitClient.fetchFilesBetweenCommits(gitConfig,
        GitFilesBetweenCommitsRequest.builder()
            .newCommitId(newCommitId)
            .oldCommitId(oldCommitId)
            .gitConnectorId(connectorId)
            .build());
  }

  @Override
  public GitFetchFilesResult fetchFilesByPath(GitConfig gitConfig, String connectorId, String commitId, String branch,
      List<String> filePaths, boolean useBranch, List<String> fileExtensions, boolean isRecursive) {
    return gitClient.fetchFilesByPath(gitConfig,
        GitFetchFilesRequest.builder()
            .commitId(commitId)
            .branch(branch)
            .filePaths(filePaths)
            .gitConnectorId(connectorId)
            .useBranch(useBranch)
            .fileExtensions(fileExtensions)
            .recursive(isRecursive)
            .build());
  }
}
