package io.harness.git;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.UNREACHABLE_HOST;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.exception.WingsException.USER;
import static io.harness.git.model.Constants.GIT_YAML_LOG_PREFIX;
import static io.harness.validation.Validator.notEmptyCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.YamlException;
import io.harness.git.model.AuthInfo;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.DiffRequest;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.FetchFilesBwCommitsRequest;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.JgitSshAuthRequest;
import io.harness.git.model.UsernamePasswordAuthRequest;
import io.harness.git.model.UsernamePasswordCredentialsProviderWithSkipSslVerify;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.TagOpt;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;

@Singleton
@Slf4j
public class GitClientV2Impl implements GitClientV2 {
  @Inject private GitClientHelper gitClientHelper;

  private void cleanup(GitBaseRequest request) {
    if (isEmpty(request.getRepoType())) {
      logger.error("gitRepoType can not be null. defaulting it to YAML");
      request.setRepoType("YAML");
    }
  }

  @Override
  public void ensureRepoLocallyClonedAndUpdated(GitBaseRequest request) {
    notNullCheck("Repo update request cannot be null", request);
    cleanup(request);
    File repoDir = new File(gitClientHelper.getRepoDirectory(request));
    boolean executionFailed = false;
    if (repoDir.exists()) {
      // Check URL change (ssh, https) and update in .git/config
      updateRemoteOriginInConfig(request.getRepoUrl(), repoDir);

      try (Git git = Git.open(repoDir)) {
        logger.info(gitClientHelper.getGitLogMessagePrefix(request.getRepoType())
            + "Repo exist. do hard sync with remote branch");

        ((FetchCommand) (getAuthConfiguredCommand(git.fetch(), request))).setTagOpt(TagOpt.FETCH_TAGS).call();
        checkout(request);

        // Do not sync to the HEAD of the branch if a specific commit SHA is provided
        if (StringUtils.isEmpty(request.getCommitId())) {
          git.reset().setMode(ResetCommand.ResetType.HARD).setRef("refs/remotes/origin/" + request.getBranch()).call();
        }
        logger.info(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Hard reset done for branch "
            + request.getBranch());
        // TODO:: log failed commits queued and being ignored.
        return;
      } catch (Exception ex) {
        executionFailed = true;
        if (ex instanceof IOException) {
          logger.error(gitClientHelper.getGitLogMessagePrefix(request.getRepoType())
                  + "Repo doesn't exist locally [repo: {}], {} ",
              request.getRepoUrl(), ex);
        } else {
          if (ex instanceof GitAPIException) {
            logger.info(
                gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Hard reset failed for branch [{}]",
                request.getBranch());
            logger.error(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Exception: ", ex);
            gitClientHelper.checkIfGitConnectivityIssue(ex);
          }
        }
      } finally {
        if (executionFailed) {
          // ensureRepoLocallyClonedAndUpdated is called before any git op (commitAndPush, diff)
          // This is synchronized on this singleton class object. So if we are inside in this method, there is
          // no other method inside this one at the same time. Also all callers are synchronized as well.
          // Means if we fail due to existing index.lock it has to be orphan lock file
          // and needs to be deleted.
          gitClientHelper.releaseLock(request, gitClientHelper.getRepoDirectory(request));
        }
      }
    }

    // We are here, so either repo doesnt exist or we encounter some error while
    // opening/updating repo
    logger.info(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Do a fresh clone");
    clone(request, gitClientHelper.getRepoDirectory(request), false);
  }

  private synchronized void clone(GitBaseRequest request, String gitRepoDirectory, boolean noCheckout) {
    try {
      if (new File(gitRepoDirectory).exists()) {
        FileUtils.deleteDirectory(new File(gitRepoDirectory));
      }
    } catch (IOException ioex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception while deleting repo: ", getMessage(ioex));
    }

    logger.info(GIT_YAML_LOG_PREFIX + "cloning repo, Git repo directory :{}", gitRepoDirectory);

    CloneCommand cloneCommand = (CloneCommand) getAuthConfiguredCommand(Git.cloneRepository(), request);
    try (Git git = cloneCommand.setURI(request.getRepoUrl())
                       .setDirectory(new File(gitRepoDirectory))
                       .setBranch(isEmpty(request.getBranch()) ? null : request.getBranch())
                       // if set to <code>true</code> no branch will be checked out, after the clone.
                       // This enhances performance of the clone command when there is no need for a checked out branch.
                       .setNoCheckout(noCheckout)
                       .call()) {
    } catch (GitAPIException ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Error in cloning repo: ", ex);
      gitClientHelper.checkIfGitConnectivityIssue(ex);
      throw new YamlException("Error in cloning repo", USER);
    }
  }

  private synchronized void checkout(GitBaseRequest request) {
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(request)))) {
      try {
        if (isNotEmpty(request.getBranch())) {
          Ref ref = git.checkout()
                        .setCreateBranch(true)
                        .setName(request.getBranch())
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setStartPoint("origin/" + request.getBranch())
                        .call();
        }

      } catch (RefAlreadyExistsException refExIgnored) {
        logger.info(
            gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Reference already exist do nothing.");
        // TODO:: check gracefully instead of relying on Exception
      }

      String gitRef = request.getCommitId() != null ? request.getCommitId() : request.getBranch();
      if (StringUtils.isNotEmpty(gitRef)) {
        git.checkout().setName(gitRef).call();
      }

    } catch (IOException | GitAPIException ex) {
      logger.error(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Exception: ", ex);
      throw new YamlException(format("Unable to checkout given reference: %s",
                                  isEmpty(request.getCommitId()) ? request.getBranch() : request.getCommitId()),
          USER);
    }
  }

  private void updateRemoteOriginInConfig(String repoUrl, File gitRepoDirectory) {
    try (Git git = Git.open(gitRepoDirectory)) {
      StoredConfig config = git.getRepository().getConfig();
      // Update local remote url if its changed
      if (!repoUrl.equals(config.getString("remote", "origin", "url"))) {
        config.setString("remote", "origin", "url", repoUrl);
        config.save();
        logger.info(GIT_YAML_LOG_PREFIX + "Local repo remote origin is updated to : {}", repoUrl);
      }
    } catch (IOException ioex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Failed to update repo url in git config", ioex);
    }
  }

  @Override
  public String validate(GitBaseRequest request) {
    notNullCheck("Validate request cannot be null", request);
    cleanup(request);
    notEmptyCheck("url cannot be empty", request.getRepoUrl());
    String repoUrl = request.getRepoUrl();
    try {
      // Init Git repo
      LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository();
      lsRemoteCommand = (LsRemoteCommand) getAuthConfiguredCommand(lsRemoteCommand, request);
      Collection<Ref> refs = lsRemoteCommand.setRemote(repoUrl).setHeads(true).setTags(true).call();
      logger.info(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Remote branches [{}]", refs);
    } catch (Exception e) {
      logger.info(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Git validation failed [{}]", e);

      if (e instanceof InvalidRemoteException || e.getCause() instanceof NoRemoteRepositoryException) {
        return "Invalid git repo " + repoUrl;
      }

      if (e instanceof org.eclipse.jgit.api.errors.TransportException) {
        org.eclipse.jgit.api.errors.TransportException te = (org.eclipse.jgit.api.errors.TransportException) e;
        Throwable cause = te.getCause();
        if (cause instanceof TransportException) {
          TransportException tee = (TransportException) cause;
          if (tee.getCause() instanceof UnknownHostException) {
            return UNREACHABLE_HOST.getDescription() + repoUrl;
          }
        }
      }
      // Any generic error
      return getMessage(e);
    }
    return null; // no error
  }

  @Override
  public void diff(DiffRequest request) {}

  @Override
  public void commitAndPush(CommitAndPushRequest request) {}

  @Override
  public void fetchFilesByPath(FetchFilesByPathRequest request) {}

  @Override
  public void fetchFilesBetweenCommits(FetchFilesBwCommitsRequest request) {}

  @Override
  public void downloadFiles(DownloadFilesRequest request) {}

  private TransportCommand getAuthConfiguredCommand(TransportCommand gitCommand, GitBaseRequest gitBaseRequest) {
    if (gitBaseRequest.getAuthRequest().getAuthType() == AuthInfo.AuthType.HTTP_PASSWORD) {
      UsernamePasswordAuthRequest authRequest = (UsernamePasswordAuthRequest) gitBaseRequest.getAuthRequest();
      gitCommand.setCredentialsProvider(new UsernamePasswordCredentialsProviderWithSkipSslVerify(
          authRequest.getUsername(), authRequest.getPassword()));
    } else if (gitBaseRequest.getAuthRequest().getAuthType() == AuthInfo.AuthType.SSH_KEY) {
      JgitSshAuthRequest authRequest = (JgitSshAuthRequest) gitBaseRequest.getAuthRequest();
      gitCommand.setTransportConfigCallback(transport -> {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(authRequest.getFactory());
      });
    }

    return gitCommand;
  }
}
