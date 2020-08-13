package io.harness.git;

import static io.harness.eraro.ErrorCode.GIT_CONNECTION_ERROR;
import static io.harness.exception.WingsException.ADMIN_SRE;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.git.model.Constants.GIT_DEFAULT_LOG_PREFIX;
import static io.harness.git.model.Constants.GIT_HELM_LOG_PREFIX;
import static io.harness.git.model.Constants.GIT_REPO_BASE_DIR;
import static io.harness.git.model.Constants.GIT_TERRAFORM_LOG_PREFIX;
import static io.harness.git.model.Constants.GIT_TRIGGER_LOG_PREFIX;
import static io.harness.git.model.Constants.GIT_YAML_LOG_PREFIX;
import static io.harness.govern.Switch.unhandled;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

import com.google.inject.Singleton;

import io.harness.exception.GitClientException;
import io.harness.exception.GitConnectionDelegateException;
import io.harness.exception.YamlException;
import io.harness.filesystem.FileIo;
import io.harness.git.model.GitBaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.TransportException;

import java.io.File;
import java.security.MessageDigest;
import javax.validation.constraints.NotNull;

@Singleton
@Slf4j
public class GitClientHelper {
  String getGitLogMessagePrefix(String repositoryType) {
    if (repositoryType == null) {
      return GIT_DEFAULT_LOG_PREFIX;
    }

    switch (repositoryType) {
      case "TERRAFORM":
        return GIT_TERRAFORM_LOG_PREFIX;

      case "YAML":
        return GIT_YAML_LOG_PREFIX;

      case "TRIGGER":
        return GIT_TRIGGER_LOG_PREFIX;

      case "HELM":
        return GIT_HELM_LOG_PREFIX;

      default:
        unhandled(repositoryType);
        return GIT_DEFAULT_LOG_PREFIX;
    }
  }

  String getRepoDirectory(GitBaseRequest request) {
    String repoName = getRepoName(request.getRepoUrl());
    String repoUrlHash = getRepoUrlHash(request.getRepoUrl());

    return buildGitRepoBaseDir(
        request.getAccountId(), request.getConnectorId(), repoName, repoUrlHash, request.getRepoType());
  }

  @NotNull
  private String buildGitRepoBaseDir(
      String accountId, String connectorId, String repoName, String repoUrlHash, String repoType) {
    return GIT_REPO_BASE_DIR.replace("${ACCOUNT_ID}", accountId)
        .replace("${REPO_TYPE}", repoType.toLowerCase())
        .replace("${CONNECTOR_ID}", connectorId)
        .replace("${REPO_NAME}", repoName)
        .replace("${REPO_URL_HASH}", repoUrlHash);
  }

  private String getRepoName(String repoUrl) {
    // TODO:: support more url types and validation);
    return repoUrl.substring(repoUrl.lastIndexOf('/') + 1).split("\\.")[0];
  }

  private String getRepoUrlHash(String repoUrl) {
    return calculateHash(repoUrl);
  }

  private String calculateHash(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest(input.getBytes());
      return encodeHexString(messageDigest);
    } catch (Exception e) {
      throw new YamlException(String.format("Error while calculating hash for input [%s].", input), e, ADMIN_SRE);
    }
  }

  public synchronized void releaseLock(GitBaseRequest request, String repoDirectory) {
    try {
      File repoDir = new File(repoDirectory);
      File file = new File(repoDir.getAbsolutePath() + "/.git/index.lock");
      FileIo.deleteFileIfExists(file.getAbsolutePath());
    } catch (Exception e) {
      logger.error(new StringBuilder(64)
                       .append("Failed to delete index.lock file for account: ")
                       .append(request.getAccountId())
                       .append(", Repo URL: ")
                       .append(request.getRepoUrl())
                       .append(", Branch: ")
                       .append(request.getBranch())
                       .toString());

      throw new GitClientException("GIT_SYNC_ISSUE: Failed to delete index.lock file", SRE, e);
    }
  }

  public void checkIfGitConnectivityIssue(Exception ex) {
    // These are the common error we find while delegate runs git command
    // TransportException is subclass of GitAPIException. This is thrown when there is any issue in connecting to git
    // repo, like invalid authorization and invalid repo

    // MissingObjectException is caused when some object(commit/ref) is missing in the git history
    if ((ex instanceof GitAPIException && ex.getCause() instanceof TransportException)
        || ex instanceof JGitInternalException || ex instanceof MissingObjectException) {
      throw new GitConnectionDelegateException(GIT_CONNECTION_ERROR, ex.getCause(), ex.getMessage(), USER_ADMIN);
    }
  }
}
