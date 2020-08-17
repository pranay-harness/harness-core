package io.harness.git;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.UNREACHABLE_HOST;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.exception.WingsException.ADMIN_SRE;
import static io.harness.exception.WingsException.USER;
import static io.harness.git.Constants.GIT_YAML_LOG_PREFIX;
import static io.harness.git.model.DiffResult.diffResultBuilder;
import static io.harness.validation.Validator.notEmptyCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.GitClientException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.YamlException;
import io.harness.filesystem.FileIo;
import io.harness.git.model.AuthInfo;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.DiffRequest;
import io.harness.git.model.DiffResult;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.FetchFilesBwCommitsRequest;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitFile;
import io.harness.git.model.GitFileChange;
import io.harness.git.model.JgitSshAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.ResetCommand;
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
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
          git.checkout()
              .setCreateBranch(true)
              .setName(request.getBranch())
              .setUpstreamMode(SetupUpstreamMode.TRACK)
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
  public DiffResult diff(DiffRequest request) {
    String startCommitIdStr = request.getLastProcessedCommitId();
    final String endCommitIdStr = StringUtils.defaultIfEmpty(request.getEndCommitId(), "HEAD");

    ensureRepoLocallyClonedAndUpdated(request);

    DiffResult diffResult = diffResultBuilder()
                                .branch(request.getBranch())
                                .repoName(request.getRepoUrl())
                                .accountId(request.getAccountId())
                                .build();
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(request)))) {
      git.checkout().setName(request.getBranch()).call();
      performGitPull(request, git);
      Repository repository = git.getRepository();

      ObjectId endCommitId = requireNonNull(repository.resolve(endCommitIdStr));
      diffResult.setCommitId(endCommitId.getName());

      // Find oldest commit
      if (startCommitIdStr == null) {
        try (RevWalk revWalk = new RevWalk(repository)) {
          RevCommit headRevCommit = revWalk.parseCommit(endCommitId);
          revWalk.sort(RevSort.REVERSE);
          revWalk.markStart(headRevCommit);
          RevCommit firstCommit = revWalk.next();
          startCommitIdStr = firstCommit.getName();
        }
      }
      logger.info(GIT_YAML_LOG_PREFIX + "startCommitIdStr =[{}], endCommitIdStr=[{}], endCommitId.name=[{}]",
          startCommitIdStr, endCommitIdStr, endCommitId.name());

      ObjectId endCommitTreeId = repository.resolve(endCommitIdStr + "^{tree}");
      ObjectId startCommitTreeId = repository.resolve(startCommitIdStr + "^{tree}");

      // ensure endCommitTreeId is after start commit
      final boolean commitsInOrder = ensureCommitOrdering(startCommitIdStr, endCommitIdStr, repository);
      if (!commitsInOrder) {
        throw new YamlException(String.format("Git diff failed. End Commit [%s] should be after start commit [%s]",
                                    endCommitIdStr, startCommitIdStr),
            ErrorCode.GIT_DIFF_COMMIT_NOT_IN_ORDER, ADMIN_SRE);
      }

      diffResult.setCommitTimeMs(getCommitTimeMs(endCommitIdStr, repository));
      diffResult.setCommitMessage(getCommitMessage(endCommitIdStr, repository));

      try (ObjectReader reader = repository.newObjectReader()) {
        CanonicalTreeParser startTreeIter = new CanonicalTreeParser();
        startTreeIter.reset(reader, startCommitTreeId);
        CanonicalTreeParser endTreeIter = new CanonicalTreeParser();
        endTreeIter.reset(reader, endCommitTreeId);

        List<DiffEntry> diffs = git.diff().setNewTree(endTreeIter).setOldTree(startTreeIter).call();
        addToGitDiffResult(diffs, diffResult, endCommitId, request.getAccountId(), repository,
            request.isExcludeFilesOutsideSetupFolder(), diffResult.getCommitTimeMs(),
            getTruncatedCommitMessage(diffResult.getCommitMessage()));
      }

    } catch (IOException | GitAPIException ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
      gitClientHelper.checkIfGitConnectivityIssue(ex);
      throw new YamlException("Error in getting commit diff", ADMIN_SRE);
    }
    return diffResult;
  }

  @VisibleForTesting
  void performGitPull(DiffRequest request, Git git) throws GitAPIException {
    ((PullCommand) (getAuthConfiguredCommand(git.pull(), request))).call();
  }

  private String getTruncatedCommitMessage(String commitMessage) {
    if (isBlank(commitMessage)) {
      return commitMessage;
    }
    return commitMessage.substring(0, Math.min(commitMessage.length(), 500));
  }

  @VisibleForTesting
  void addToGitDiffResult(List<DiffEntry> diffs, DiffResult diffResult, ObjectId headCommitId, String accountId,
      Repository repository, boolean excludeFilesOutsideSetupFolder, Long commitTimeMs, String commitMessage)
      throws IOException {
    logger.info(GIT_YAML_LOG_PREFIX + "Diff Entries: {}", diffs);
    ArrayList<GitFileChange> gitFileChanges = new ArrayList<>();
    for (DiffEntry entry : diffs) {
      String content = null;
      String filePath;
      ObjectId objectId;
      if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
        filePath = entry.getOldPath();
        // we still want to collect content for deleted file, as it will be needed to decide yamlhandlerSubType in
        // many cases. so getting oldObjectId
        objectId = entry.getOldId().toObjectId();
      } else {
        filePath = entry.getNewPath();
        objectId = entry.getNewId().toObjectId();
      }

      if (excludeFilesOutsideSetupFolder && filePath != null && !filePath.startsWith("SETUP_FOLDER")) {
        logger.info("Excluding file [{}] ", filePath);
        continue;
      }

      ObjectLoader loader = repository.open(objectId);
      content = new String(loader.getBytes(), StandardCharsets.UTF_8);
      GitFileChange gitFileChange = GitFileChange.builder()
                                        .commitId(headCommitId.getName())
                                        .changeType(gitClientHelper.getChangeType(entry.getChangeType()))
                                        .filePath(filePath)
                                        .fileContent(content)
                                        .objectId(objectId.name())
                                        .accountId(accountId)
                                        .commitTimeMs(commitTimeMs)
                                        .commitMessage(commitMessage)
                                        .build();

      gitFileChanges.add(gitFileChange);
    }
    diffResult.setGitFileChanges(gitFileChanges);
  }

  private Long getCommitTimeMs(String endCommitIdStr, Repository repository) throws IOException {
    try (RevWalk revWalk = new RevWalk(repository)) {
      final RevCommit endCommit = revWalk.parseCommit(repository.resolve(endCommitIdStr));
      return endCommit != null ? endCommit.getCommitTime() * 1000L : null;
    }
  }

  private String getCommitMessage(String endCommitIdStr, Repository repository) throws IOException {
    try (RevWalk revWalk = new RevWalk(repository)) {
      final RevCommit endCommit = revWalk.parseCommit(repository.resolve(endCommitIdStr));
      return endCommit != null ? endCommit.getFullMessage() : null;
    }
  }

  @VisibleForTesting
  boolean ensureCommitOrdering(String startCommitIdStr, String endCommitIdStr, Repository repository)
      throws IOException {
    try (RevWalk revWalk = new RevWalk(repository)) {
      final RevCommit startCommit = revWalk.parseCommit(repository.resolve(startCommitIdStr));
      final RevCommit endCommit = revWalk.parseCommit(repository.resolve(endCommitIdStr));
      return endCommit.getCommitTime() >= startCommit.getCommitTime();
    }
  }

  @Override
  public void commitAndPush(CommitAndPushRequest request) {}

  @Override
  public void fetchFilesBetweenCommits(FetchFilesBwCommitsRequest request) {}

  @Override
  public List<GitFile> fetchFilesByPath(FetchFilesByPathRequest request) {
    cleanup(request);
    validateRequiredArgs(request);

    synchronized (gitClientHelper.getLockObject(request.getConnectorId())) {
      try {
        checkoutFiles(request);
        List<GitFile> gitFiles = getFilteredGitFiles(request);
        resetWorkingDir(request);

        if (isNotEmpty(gitFiles)) {
          gitFiles.forEach(gitFile -> logger.info("File fetched : " + gitFile.getFilePath()));
        }
        return gitFiles;

      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        logger.error(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Exception: ", e);
        throw new YamlException(new StringBuilder()
                                    .append("Failed while fetching files ")
                                    .append(request.useBranch() ? "for Branch: " : "for CommitId: ")
                                    .append(request.useBranch() ? request.getBranch() : request.getCommitId())
                                    .append(", FilePaths: ")
                                    .append(request.getFilePaths())
                                    .toString(),
            USER);
      }
    }
  }

  @VisibleForTesting
  public List<GitFile> getFilteredGitFiles(FetchFilesByPathRequest request) {
    List<GitFile> gitFiles = new ArrayList<>();

    String repoPath = gitClientHelper.getFileDownloadRepoDirectory(request);
    request.getFilePaths().forEach(filePath -> {
      try {
        Path repoFilePath = Paths.get(repoPath + "/" + filePath);
        Stream<Path> paths = request.isRecursive() ? Files.walk(repoFilePath) : Files.walk(repoFilePath, 1);
        paths.filter(Files::isRegularFile)
            .filter(path -> !path.toString().contains(".git"))
            .filter(matchingFilesExtensions(request.getFileExtensions()))
            .forEach(path -> gitClientHelper.addFiles(gitFiles, path, repoPath));
      } catch (Exception e) {
        resetWorkingDir(request);

        // GitFetchFilesTask relies on the exception cause whether to fail the deployment or not.
        // If the exception is being changed, make sure that the throwable cause is added to the new exception
        // Unit test testGetFilteredGitFilesNoFileFoundException makes sure that the original exception is not swallowed
        throw new GitClientException(new StringBuilder("Unable to checkout files for filePath [")
                                         .append(filePath)
                                         .append("]")
                                         .append(request.useBranch() ? "for Branch: " : "for CommitId: ")
                                         .append(request.useBranch() ? request.getBranch() : request.getCommitId())
                                         .toString(),
            USER, e);
      }
    });

    return gitFiles;
  }

  private Predicate<Path> matchingFilesExtensions(List<String> fileExtensions) {
    return path -> {
      if (isEmpty(fileExtensions)) {
        return true;
      } else {
        for (String fileExtension : fileExtensions) {
          if (path.toString().endsWith(fileExtension)) {
            return true;
          }
        }
        return false;
      }
    };
  }

  @Override
  public void downloadFiles(DownloadFilesRequest request) {
    cleanup(request);
    validateRequiredArgs(request);

    synchronized (gitClientHelper.getLockObject(request.getConnectorId())) {
      try {
        checkoutFiles(request);
        String repoPath = gitClientHelper.getFileDownloadRepoDirectory(request);

        FileIo.createDirectoryIfDoesNotExist(request.getDestinationDirectory());
        FileIo.waitForDirectoryToBeAccessibleOutOfProcess(request.getDestinationDirectory(), 10);

        File destinationDir = new File(request.getDestinationDirectory());

        for (String filePath : request.getFilePaths()) {
          File sourceDir = new File(Paths.get(repoPath + "/" + filePath).toString());
          if (sourceDir.isFile()) {
            FileUtils.copyFile(sourceDir, Paths.get(request.getDestinationDirectory(), filePath).toFile());
          } else {
            FileUtils.copyDirectory(sourceDir, destinationDir);
          }
        }

        resetWorkingDir(request);
      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        throw new YamlException(new StringBuilder()
                                    .append("Failed while fetching files ")
                                    .append(request.useBranch() ? "for Branch: " : "for CommitId: ")
                                    .append(request.useBranch() ? request.getBranch() : request.getCommitId())
                                    .append(", FilePaths: ")
                                    .append(request.getFilePaths())
                                    .append(". Reason: ")
                                    .append(e.getMessage())
                                    .toString(),
            e, USER);
      }
    }
  }

  private void resetWorkingDir(GitBaseRequest request) {
    try (Git git = Git.open(new File(gitClientHelper.getFileDownloadRepoDirectory(request)))) {
      logger.info("Resetting repo");
      ResetCommand resetCommand = new ResetCommand(git.getRepository()).setMode(ResetCommand.ResetType.HARD);
      resetCommand.call();
      logger.info("Resetting repo completed successfully");
    } catch (Exception ex) {
      logger.error(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Exception: ", ex);
      gitClientHelper.checkIfGitConnectivityIssue(ex);
      throw new YamlException("Error in resetting repo", USER);
    }
  }

  private void checkoutFiles(FetchFilesByPathRequest request) {
    synchronized (gitClientHelper.getLockObject(request.getConnectorId())) {
      logger.info(new StringBuilder(128)
                      .append(" Processing Git command: FETCH_FILES ")
                      .append("Account: ")
                      .append(request.getAccountId())
                      .append(", repo: ")
                      .append(request.getRepoUrl())
                      .append(request.useBranch() ? ", Branch: " : ", CommitId: ")
                      .append(request.useBranch() ? request.getBranch() : request.getCommitId())
                      .append(", filePaths: ")
                      .append(request.getFilePaths())
                      .toString());

      gitClientHelper.createDirStructureForFileDownload(request);

      // clone repo locally without checkout
      cloneRepoForFilePathCheckout(request);

      // if useBranch is set, use it to checkout latest, else checkout given commitId
      if (request.useBranch()) {
        checkoutBranchForPath(request);
      } else {
        checkoutGivenCommitForPath(request);
      }
    }
  }

  private void checkoutGivenCommitForPath(FetchFilesByPathRequest request) {
    try (Git git = Git.open(new File(gitClientHelper.getFileDownloadRepoDirectory(request)))) {
      logger.info("Checking out commitId: " + request.getCommitId());
      CheckoutCommand checkoutCommand = git.checkout().setStartPoint(request.getCommitId()).setCreateBranch(false);

      setPathsForCheckout(request.getFilePaths(), checkoutCommand);
      checkoutCommand.call();
      logger.info("Successfully Checked out commitId: " + request.getCommitId());
    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
      gitClientHelper.checkIfGitConnectivityIssue(ex);
      throw new YamlException("Error in checking out commit id " + request.getCommitId(), USER);
    }
  }

  private void checkoutBranchForPath(FetchFilesByPathRequest request) {
    try (Git git = Git.open(new File(gitClientHelper.getFileDownloadRepoDirectory(request)))) {
      logger.info("Checking out Branch: " + request.getBranch());
      CheckoutCommand checkoutCommand = git.checkout()
                                            .setCreateBranch(true)
                                            .setStartPoint("origin/" + request.getBranch())
                                            .setForce(true)
                                            .setUpstreamMode(SetupUpstreamMode.TRACK)
                                            .setName(request.getBranch());

      setPathsForCheckout(request.getFilePaths(), checkoutCommand);
      checkoutCommand.call();
      logger.info("Successfully Checked out Branch: " + request.getBranch());
    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
      gitClientHelper.checkIfGitConnectivityIssue(ex);
      throw new YamlException("Error in checking out Branch " + request.getBranch(), USER);
    }
  }

  private void setPathsForCheckout(List<String> filePaths, CheckoutCommand checkoutCommand) {
    if (filePaths.size() == 1 && filePaths.get(0).equals("")) {
      checkoutCommand.setAllPaths(true);
    } else {
      filePaths.forEach(checkoutCommand::addPath);
    }
  }

  /**
   * Ensure repo locally cloned. This is called before performing any git operation with remote
   * @param request
   */
  private synchronized void cloneRepoForFilePathCheckout(GitBaseRequest request) {
    logger.info(new StringBuilder(64)
                    .append(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()))
                    .append("Cloning repo without checkout for file fetch op, for GitConfig: ")
                    .append(request.toString())
                    .toString());

    boolean exceptionOccured = false;
    File repoDir = new File(gitClientHelper.getFileDownloadRepoDirectory(request));
    // If repo already exists, update references
    if (repoDir.exists()) {
      // Check URL change (ssh, https) and update in .git/config
      updateRemoteOriginInConfig(request.getRepoUrl(), repoDir);

      try (Git git = Git.open(repoDir)) {
        // update ref with latest commits on remote
        FetchResult fetchResult = ((FetchCommand) (getAuthConfiguredCommand(git.fetch(), request)))
                                      .setRemoveDeletedRefs(true)
                                      .setTagOpt(TagOpt.FETCH_TAGS)
                                      .call(); // fetch all remote references

        logger.info(new StringBuilder()
                        .append(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()))
                        .append("result fetched: ")
                        .append(fetchResult.toString())
                        .toString());

        return;
      } catch (Exception ex) {
        exceptionOccured = true;
        if (ex instanceof IOException) {
          logger.warn(gitClientHelper.getGitLogMessagePrefix(request.getRepoType())
                  + "Repo doesn't exist locally [repo: {}], {} ",
              request.getRepoUrl(), ex);
          logger.info(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Do a fresh clone");
        } else {
          logger.info(
              gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Hard reset failed for branch [{}]",
              request.getBranch());
          logger.error(gitClientHelper.getGitLogMessagePrefix(request.getRepoType()) + "Exception: ", ex);
          gitClientHelper.checkIfGitConnectivityIssue(ex);
        }
      } finally {
        if (exceptionOccured) {
          gitClientHelper.releaseLock(request, gitClientHelper.getFileDownloadRepoDirectory(request));
        }
      }
    }

    clone(request, gitClientHelper.getFileDownloadRepoDirectory(request), true);
  }

  /**
   * FilePath cant empty as well as (Branch and commitId both cant be empty)
   *
   * @param request Download request
   * @throws InvalidRequestException for required args with message
   */
  private void validateRequiredArgs(FetchFilesByPathRequest request) {
    if (isEmpty(request.getFilePaths())) {
      throw new InvalidRequestException("FilePaths can not be empty", USER);
    }

    if (isEmpty(request.getBranch()) && isEmpty(request.getCommitId())) {
      throw new InvalidRequestException("No refs provided to checkout", USER);
    }
  }

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
