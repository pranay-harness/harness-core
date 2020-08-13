package io.harness.cdng.gitclient;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.UNREACHABLE_HOST;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.exception.WingsException.ADMIN;
import static io.harness.exception.WingsException.ADMIN_SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE;
import static software.wings.beans.GitConfig.HARNESS_IO_KEY_;
import static software.wings.beans.GitConfig.HARNESS_SUPPORT_EMAIL_KEY;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitConnectionType;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.gitconnector.GitSyncConfig;
import io.harness.delegate.beans.git.GitCheckoutResult;
import io.harness.delegate.beans.git.GitCloneResult;
import io.harness.delegate.beans.git.GitCommitAndPushRequest;
import io.harness.delegate.beans.git.GitCommitAndPushResult;
import io.harness.delegate.beans.git.GitCommitResult;
import io.harness.delegate.beans.git.GitFileChange;
import io.harness.delegate.beans.git.GitFileChange.ChangeType;
import io.harness.delegate.beans.git.GitPushResult;
import io.harness.exception.GitClientException;
import io.harness.exception.YamlException;
import io.harness.git.model.UsernamePasswordCredentialsProviderWithSkipSslVerify;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
public class GitClientNGImpl implements GitClientNG {
  @Inject GitClientHelperNG gitClientHelper;

  @Override
  public String validate(GitConfigDTO gitConfig) {
    if (GitConnectionType.REPO == gitConfig.getGitAuth().getGitConnectionType()) {
      try {
        // Init Git repo
        initGitAndGetBranches(gitConfig);
      } catch (TransportException te) {
        logger.info("Git validation failed [{}]", te);
        Throwable cause = te.getCause();
        if (cause instanceof TransportException) {
          TransportException tee = (TransportException) cause;
          if (tee.getCause() instanceof UnknownHostException) {
            return UNREACHABLE_HOST.getDescription() + gitConfig.getGitAuth().getUrl();
          }
        }
      } catch (InvalidRemoteException e) {
        return buildInvalidGitRepoErrorMessage(gitConfig, e);
      } catch (Exception e) {
        if (e.getCause() instanceof NoRemoteRepositoryException) {
          return buildInvalidGitRepoErrorMessage(gitConfig, e);
        }
        return getMessage(e);
      }
    } else {
      // TODO @deepak: implement account level git connector.
      return "Not implemented";
    }
    return null; // no error
  }

  @NotNull
  private String buildInvalidGitRepoErrorMessage(GitConfigDTO gitConfig, Exception e) {
    logger.info("Git validation failed.", e);
    return "Invalid git repo " + gitConfig.getGitAuth().getUrl();
  }

  @VisibleForTesting
  void initGitAndGetBranches(GitConfigDTO gitConfig) throws GitAPIException {
    LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository();
    getAuthConfiguredCommand(lsRemoteCommand, gitConfig);
    Collection<Ref> refs =
        lsRemoteCommand.setRemote(gitConfig.getGitAuth().getUrl()).setHeads(true).setTags(true).call();
    logger.info("Remote branches [{}]", refs);
  }

  @VisibleForTesting
  TransportCommand getAuthConfiguredCommand(TransportCommand gitCommand, GitConfigDTO gitConfig) {
    if (gitConfig.getGitAuth().getUrl().toLowerCase().startsWith("http")) {
      configureHttpCredentialProvider(gitCommand, (GitHTTPAuthenticationDTO) gitConfig.getGitAuth());
    } else {
      throw new NotImplementedException("Not implemented ssh");
    }
    return gitCommand;
  }

  private void configureHttpCredentialProvider(TransportCommand gitCommand, GitHTTPAuthenticationDTO gitAuth) {
    String username = gitAuth.getUsername();
    char[] password = gitAuth.getPasswordRef().getDecryptedValue();
    // todo @deepak: add kerberos later
    gitCommand.setCredentialsProvider(new UsernamePasswordCredentialsProviderWithSkipSslVerify(username, password));
  }

  @Override
  public synchronized GitCommitAndPushResult commitAndPush(
      GitConfigDTO gitConfigDTO, GitCommitAndPushRequest gitCommitRequest, String accountId, String reference) {
    GitCommitResult commitResult = commit(gitConfigDTO, gitCommitRequest, accountId, reference);
    GitCommitAndPushResult gitCommitAndPushResult =
        GitCommitAndPushResult.builder().gitCommitResult(commitResult).build();
    if (isNotBlank(commitResult.getCommitId())) {
      gitCommitAndPushResult.setGitPushResult(push(gitConfigDTO, gitCommitRequest, accountId));
      List<GitFileChange> gitFileChanges = getFilesCommited(commitResult.getCommitId(), gitConfigDTO, accountId);
      gitCommitAndPushResult.setFilesCommitedToGit(gitFileChanges);
    } else {
      logger.warn("Null commitId. Nothing to push for request [{}]", gitCommitRequest);
    }
    return gitCommitAndPushResult;
  }

  @VisibleForTesting
  List<GitFileChange> getFilesCommited(String gitCommitId, GitConfigDTO gitConfig, String accountId) {
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig, accountId)))) {
      ObjectId commitId = ObjectId.fromString(gitCommitId);
      RevCommit currentCommitObject = null;
      try (RevWalk revWalk = new RevWalk(git.getRepository())) {
        currentCommitObject = revWalk.parseCommit(commitId);
      }

      if (currentCommitObject == null) {
        String repoURL = StringUtils.defaultIfBlank(gitConfig.getGitAuth().getUrl(), "");
        throw new GitClientException(
            String.format("No commit was found with the commitId (%s) in git repo (%s)", gitCommitId, repoURL), USER,
            null);
      }

      RevCommit parentCommit = currentCommitObject.getParent(0);

      ObjectId newCommitHead = git.getRepository().resolve(currentCommitObject.getName() + "^{tree}");
      ObjectId oldCommitHead = null;
      if (parentCommit != null) {
        oldCommitHead = git.getRepository().resolve(parentCommit.getName() + "^{tree}");
      }
      List<DiffEntry> diffs = getDiffEntries(git.getRepository(), git, newCommitHead, oldCommitHead);
      return getGitFileChangesFromDiff(diffs, git.getRepository(), accountId);
    } catch (Exception ex) {
      throw new YamlException("Error in getting the files commited to the git", ex, USER_ADMIN);
    }
  }

  private List<DiffEntry> getDiffEntries(Repository repository, Git git, ObjectId newCommitHead, ObjectId oldCommitHead)
      throws IOException, GitAPIException {
    try (ObjectReader reader = repository.newObjectReader()) {
      CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
      oldTreeIter.reset(reader, oldCommitHead);
      CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
      newTreeIter.reset(reader, newCommitHead);

      return git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
    }
  }

  private List<GitFileChange> getGitFileChangesFromDiff(List<DiffEntry> diffs, Repository repository, String accountId)
      throws IOException {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    for (DiffEntry entry : diffs) {
      ObjectId objectId;
      String filePath;
      if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
        filePath = entry.getOldPath();
        objectId = entry.getOldId().toObjectId();
      } else {
        filePath = entry.getNewPath();
        objectId = entry.getNewId().toObjectId();
      }
      ObjectLoader loader = repository.open(objectId);
      String content = new String(loader.getBytes(), StandardCharsets.UTF_8);
      gitFileChanges.add(GitFileChange.builder()
                             .accountId(accountId)
                             .filePath(filePath)
                             .fileContent(content)
                             .changeType(gitClientHelper.getChangeType(entry.getChangeType()))
                             .syncFromGit(false)
                             .changeFromAnotherCommit(false)
                             .build());
    }
    return gitFileChanges;
  }

  @Override
  public synchronized GitCommitResult commit(
      GitConfigDTO gitConfig, GitCommitAndPushRequest gitCommitRequest, String accountId, String reference) {
    List<String> filesToAdd = new ArrayList<>();

    ensureRepoLocallyClonedAndUpdated(gitConfig, accountId, reference);

    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig, accountId)))) {
      applyChangeSetOnFileSystem(
          gitClientHelper.getRepoDirectory(gitConfig, accountId), gitConfig, gitCommitRequest, filesToAdd, git);

      /* Removal of files should happen before addition of files. TODO:: Add test to ensure behaviour */
      applyGitAddCommand(filesToAdd, git);

      Status status = git.status().call();

      if (status.isClean()) {
        logger.warn("No git change to commit. GitCommitRequest: [{}]", gitCommitRequest);
        return GitCommitResult.builder().build(); // do nothing
      }

      StringBuilder commitMessage = gitClientHelper.prepareCommitMessage(gitConfig, status, accountId);
      Optional<String> commitName = Optional.ofNullable(gitConfig.getGitSyncConfig())
                                        .map(GitSyncConfig::getCustomCommitAttributes)
                                        .map(CustomCommitAttributes::getAuthorName);
      String authorName = commitName.isPresent() && isNotBlank(commitName.get()) ? commitName.get() : HARNESS_IO_KEY_;
      Optional<String> mailId = Optional.ofNullable(gitConfig.getGitSyncConfig())
                                    .map(GitSyncConfig::getCustomCommitAttributes)
                                    .map(CustomCommitAttributes::getAuthorEmail);
      String authorEmailId = mailId.isPresent() && isNotBlank(mailId.get()) ? mailId.get() : HARNESS_SUPPORT_EMAIL_KEY;

      RevCommit revCommit = git.commit()
                                .setCommitter(authorName, authorEmailId)
                                .setAuthor(authorName, authorEmailId)
                                .setAll(true)
                                .setMessage(commitMessage.toString())
                                .call();

      return GitCommitResult.builder()
          .commitId(revCommit.getName())
          .commitTime(revCommit.getCommitTime())
          .commitMessage(gitClientHelper.getTruncatedCommitMessage(commitMessage.toString()))
          .build();

    } catch (IOException | GitAPIException ex) {
      throw new YamlException("Error in writing commit", ex, ADMIN_SRE);
    }
  }

  @VisibleForTesting
  void applyGitAddCommand(List<String> filesToAdd, Git git) {
    /*
    We do not need to specifically git add every added/modified file. git add . will take care
    of this
     */
    if (isNotEmpty(filesToAdd)) {
      try {
        git.add().addFilepattern(".").call();
      } catch (GitAPIException ex) {
        throw new YamlException("Error in add/modify git operation.", ex, ADMIN_SRE);
      }
    }
  }

  /*
      We need to ensure creation/deletion in the same order as the gitFileChanges but we do not
      want to git add each file individually because that slows down things but deleting is easy
      since we can just do git delete root_folder if some entity is deleted. Therefore, git rm is
      done here and just creation of files is done here and git add . should be done after this
      method call.
  */
  @VisibleForTesting
  void applyChangeSetOnFileSystem(String repoDirectory, GitConfigDTO gitConfig,
      GitCommitAndPushRequest gitCommitRequest, List<String> filesToAdd, Git git) {
    gitCommitRequest.getGitFileChanges().forEach(gitFileChange -> {
      String filePath = repoDirectory + PATH_DELIMITER + gitFileChange.getFilePath();
      File file = new File(filePath);
      final ChangeType changeType = gitFileChange.getChangeType();
      switch (changeType) {
        case ADD:
        case MODIFY:
          try {
            logger.info("Adding git file " + gitFileChange.toString());
            FileUtils.forceMkdir(file.getParentFile());
            FileUtils.writeStringToFile(file, gitFileChange.getFileContent(), UTF_8);
            filesToAdd.add(gitFileChange.getFilePath());
          } catch (IOException ex) {
            logger.error("Exception in adding/modifying file to git " + ex);
            throw new YamlException("IOException in ADD/MODIFY git operation", ADMIN);
          }
          break;
        case DELETE:
          File fileToBeDeleted = new File(repoDirectory + PATH_DELIMITER + gitFileChange.getFilePath());
          if (fileToBeDeleted.exists()) {
            try {
              git.rm().addFilepattern(gitFileChange.getFilePath()).call();
            } catch (GitAPIException e) {
              logger.error("Exception in deleting file " + e);
              throw new YamlException(
                  format("Exception in deleting file [%s]", gitFileChange.getFilePath()), ADMIN_SRE);
            }
            logger.info("Deleting git file " + gitFileChange.toString());
          } else {
            logger.warn("File already deleted. path: [{}]", gitFileChange.getFilePath());
          }
          break;
        default:
          unhandled(changeType);
      }
    });
  }

  public synchronized void ensureRepoLocallyClonedAndUpdated(
      GitConfigDTO gitConfig, String accountId, String reference) {
    File repoDir = new File(gitClientHelper.getRepoDirectory(gitConfig, accountId));
    boolean executionFailed = false;
    if (repoDir.exists()) {
      // Check URL change (ssh, https) and update in .git/config
      updateRemoteOriginInConfig(gitConfig, repoDir);

      try (Git git = Git.open(repoDir)) {
        logger.info("Repo exist. do hard sync with remote branch");

        FetchResult fetchResult =
            ((FetchCommand) (getAuthConfiguredCommand(git.fetch(), gitConfig))).call(); // fetch all remote references
        checkout(gitConfig, accountId, reference);

        // Do not sync to the HEAD of the branch if a specific commit SHA is provided
        if (StringUtils.isEmpty(reference)) {
          Ref ref = git.reset()
                        .setMode(ResetType.HARD)
                        .setRef("refs/remotes/origin/" + gitConfig.getGitAuth().getBranchName())
                        .call();
        }
        logger.info("Hard reset done for branch " + gitConfig.getGitAuth().getBranchName());
        // TODO:: log failed commits queued and being ignored.
        return;
      } catch (Exception ex) {
        executionFailed = true;
        if (ex instanceof IOException) {
          logger.error("Repo doesn't exist locally [repo: {}], {} ", gitConfig.getGitAuth().getUrl(), ex);
        } else {
          if (ex instanceof GitAPIException) {
            logger.info("Hard reset failed for branch [{}]", gitConfig.getGitAuth().getBranchName());
            logger.error("Exception: ", ex);
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
          gitClientHelper.releaseLock(gitConfig, gitClientHelper.getRepoDirectory(gitConfig, accountId), accountId);
        }
      }
    }

    // We are here, so either repo doesnt exist or we encounter some error while
    // opening/updating repo
    logger.info("Do a fresh clone");
    clone(gitConfig, gitClientHelper.getRepoDirectory(gitConfig, accountId), gitConfig.getGitAuth().getBranchName(),
        false);
  }

  @Override
  public synchronized GitCloneResult clone(
      GitConfigDTO gitConfig, String gitRepoDirectory, String branch, boolean noCheckout) {
    try {
      if (new File(gitRepoDirectory).exists()) {
        FileUtils.deleteDirectory(new File(gitRepoDirectory));
      }
    } catch (IOException ioex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception while deleting repo: ", getMessage(ioex));
    }

    logger.info(GIT_YAML_LOG_PREFIX + "cloning repo, Git repo directory :{}", gitRepoDirectory);

    CloneCommand cloneCommand = Git.cloneRepository();
    cloneCommand = (CloneCommand) getAuthConfiguredCommand(cloneCommand, gitConfig);
    try (Git git = cloneCommand.setURI(gitConfig.getGitAuth().getUrl())
                       .setDirectory(new File(gitRepoDirectory))
                       .setBranch(isEmpty(branch) ? null : branch)
                       // if set to <code>true</code> no branch will be checked out, after the clone.
                       // This enhances performance of the clone command when there is no need for a checked out branch.
                       .setNoCheckout(noCheckout)
                       .call()) {
      return GitCloneResult.builder().build();
    } catch (GitAPIException ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Error in cloning repo: ", ex);
      gitClientHelper.checkIfGitConnectivityIssue(ex);
      throw new YamlException("Error in cloning repo", USER);
    }
  }

  @VisibleForTesting
  void updateRemoteOriginInConfig(GitConfigDTO gitConfig, File gitRepoDirectory) {
    try (Git git = Git.open(gitRepoDirectory)) {
      StoredConfig config = git.getRepository().getConfig();
      // Update local remote url if its changed
      if (!config.getString("remote", "origin", "url").equals(gitConfig.getGitAuth().getUrl())) {
        config.setString("remote", "origin", "url", gitConfig.getGitAuth().getUrl());
        config.save();
        logger.info(GIT_YAML_LOG_PREFIX + "Local repo remote origin is updated to : ", gitConfig.getGitAuth().getUrl());
      }
    } catch (IOException ioex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Failed to update repo url in git config", ioex);
    }
  }

  @Override
  public synchronized GitCheckoutResult checkout(GitConfigDTO gitConfig, String accountId, String reference) {
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig, accountId)))) {
      try {
        if (isNotEmpty(gitConfig.getGitAuth().getBranchName())) {
          Ref ref = git.checkout()
                        .setCreateBranch(true)
                        .setName(gitConfig.getGitAuth().getBranchName())
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setStartPoint("origin/" + gitConfig.getGitAuth().getBranchName())
                        .call();
        }

      } catch (RefAlreadyExistsException refExIgnored) {
        logger.info("Reference already exist do nothing."); // TODO:: check gracefully instead of relying on Exception
      }

      String gitRef = isNotEmpty(reference) ? reference : gitConfig.getGitAuth().getBranchName();
      if (StringUtils.isNotEmpty(gitRef)) {
        git.checkout().setName(gitRef).call();
      }

      return GitCheckoutResult.builder().build();
    } catch (IOException | GitAPIException ex) {
      logger.error("Exception: ", ex);
      throw new YamlException(format("Unable to checkout given reference: %s",
                                  isEmpty(reference) ? gitConfig.getGitAuth().getBranchName() : reference),
          USER);
    }
  }

  @Override
  public synchronized GitPushResult push(
      GitConfigDTO gitConfig, GitCommitAndPushRequest gitCommitRequest, String accountId) {
    boolean forcePush = gitCommitRequest.isForcePush();

    logger.info("Performing git PUSH, forcePush is: " + forcePush);

    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig, accountId)))) {
      Iterable<PushResult> pushResults = ((PushCommand) (getAuthConfiguredCommand(git.push(), gitConfig)))
                                             .setRemote("origin")
                                             .setForce(forcePush)
                                             .setRefSpecs(new RefSpec(gitConfig.getGitAuth().getBranchName()))
                                             .call();

      RemoteRefUpdate remoteRefUpdate = pushResults.iterator().next().getRemoteUpdates().iterator().next();
      GitPushResult.RefUpdate refUpdate =
          GitPushResult.RefUpdate.builder()
              .status(remoteRefUpdate.getStatus().name())
              .expectedOldObjectId(remoteRefUpdate.getExpectedOldObjectId() != null
                      ? remoteRefUpdate.getExpectedOldObjectId().name()
                      : null)
              .newObjectId(remoteRefUpdate.getNewObjectId() != null ? remoteRefUpdate.getNewObjectId().name() : null)
              .forceUpdate(remoteRefUpdate.isForceUpdate())
              .message(remoteRefUpdate.getMessage())
              .build();
      if (remoteRefUpdate.getStatus() == OK || remoteRefUpdate.getStatus() == UP_TO_DATE) {
        return GitPushResult.builder().refUpdate(refUpdate).build();
      } else {
        String errorMsg = format("Unable to push changes to git repository [%s]. "
                + "Status reported by Remote is: %s and message is: %s. "
                + "Other info: Force push: %s. Fast forward: %s",
            gitConfig.getGitAuth().getUrl(), remoteRefUpdate.getStatus(), remoteRefUpdate.getMessage(),
            remoteRefUpdate.isForceUpdate(), remoteRefUpdate.isFastForward());
        logger.error(errorMsg);
        throw new YamlException(errorMsg, ADMIN_SRE);
      }
    } catch (IOException | GitAPIException ex) {
      logger.error("Exception: ", ex);
      String errorMsg = getMessage(ex);
      if (ex instanceof InvalidRemoteException || ex.getCause() instanceof NoRemoteRepositoryException) {
        errorMsg =
            "Invalid git repo or user doesn't have write access to repository. repo:" + gitConfig.getGitAuth().getUrl();
      }

      gitClientHelper.checkIfGitConnectivityIssue(ex);
      throw new YamlException(errorMsg, USER);
    }
  }
}
