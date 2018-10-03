package software.wings.service.impl.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.UNREACHABLE_HOST;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE;
import static software.wings.beans.yaml.YamlConstants.GIT_TERRAFORM_LOG_PREFIX;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;

import com.google.inject.Inject;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Singleton;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.errors.TransportException;
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
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitCheckoutResult;
import software.wings.beans.yaml.GitCloneResult;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitCommitResult;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitPushResult;
import software.wings.beans.yaml.GitPushResult.RefUpdate;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.utils.Misc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by anubhaw on 10/16/17.
 */

@Singleton
public class GitClientImpl implements GitClient {
  private static final String TEMP_SSH_KEY_DIR = "./repository/.ssh";
  private static final String COMMIT_TIMESTAMP_FORMAT = "yyyy.MM.dd.HH.mm.ss";

  private static final Logger logger = LoggerFactory.getLogger(GitClientImpl.class);
  @Inject GitClientHelper gitClientHelper;

  @Override
  public synchronized GitCloneResult clone(
      GitConfig gitConfig, String gitRepoDirectory, String branch, boolean noCheckout) {
    try {
      if (new File(gitRepoDirectory).exists()) {
        FileUtils.deleteDirectory(new File(gitRepoDirectory));
      }
    } catch (IOException ioex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception while deleting repo: ", Misc.getMessage(ioex));
    }

    logger.info(GIT_YAML_LOG_PREFIX + "cloning repo, Git repo directory :{}", gitRepoDirectory);

    CloneCommand cloneCommand = Git.cloneRepository();
    cloneCommand = (CloneCommand) getAuthConfiguredCommand(cloneCommand, gitConfig);
    try (Git git = cloneCommand.setURI(gitConfig.getRepoUrl())
                       .setDirectory(new File(gitRepoDirectory))
                       .setBranch(isEmpty(branch) ? null : branch)
                       // if set to <code>true</code> no branch will be checked out, after the clone.
                       // This enhances performance of the clone command when there is no need for a checked out branch.
                       .setNoCheckout(noCheckout)
                       .call()) {
      return GitCloneResult.builder().build();
    } catch (GitAPIException ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Error in cloning repo: ", ex);
      gitClientHelper.checkIfTransportException(ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR, "Error in cloning repo")
          .addParam("message", "Error in cloning repo");
    }
  }

  private void updateRemoteOriginInConfig(GitConfig gitConfig) {
    String gitRepoDirectory = gitClientHelper.getRepoDirectory(gitConfig);

    try (Git git = Git.open(new File(gitRepoDirectory))) {
      StoredConfig config = git.getRepository().getConfig();
      // Update local remote url if its changed
      if (!config.getString("remote", "origin", "url").equals(gitConfig.getRepoUrl())) {
        config.setString("remote", "origin", "url", gitConfig.getRepoUrl());
        config.save();
        logger.info(GIT_YAML_LOG_PREFIX + "Local repo remote origin is updated to : ", gitConfig.getRepoUrl());
      }
    } catch (IOException ioex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Failed to update repo url in git config");
    }
  }

  @Override
  public synchronized GitDiffResult diff(GitConfig gitConfig, String startCommitId) {
    ensureRepoLocallyClonedAndUpdated(gitConfig);

    GitDiffResult diffResult = GitDiffResult.builder()
                                   .branch(gitConfig.getBranch())
                                   .repoName(gitConfig.getRepoUrl())
                                   .gitFileChanges(new ArrayList<>())
                                   .build();
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig)))) {
      git.checkout().setName(gitConfig.getBranch()).call();
      ((PullCommand) (getAuthConfiguredCommand(git.pull(), gitConfig))).call();
      Repository repository = git.getRepository();
      ObjectId headCommitId = repository.resolve("HEAD");
      diffResult.setCommitId(headCommitId.getName());

      // Find oldest commit
      if (startCommitId == null) {
        try (RevWalk revWalk = new RevWalk(repository)) {
          RevCommit headRevCommit = revWalk.parseCommit(headCommitId);
          revWalk.sort(RevSort.REVERSE);
          revWalk.markStart(headRevCommit);
          RevCommit firstCommit = revWalk.next();
          startCommitId = firstCommit.getName();
        }
      }

      ObjectId head = repository.resolve("HEAD^{tree}");
      ObjectId oldHead = repository.resolve(startCommitId + "^{tree}");

      try (ObjectReader reader = repository.newObjectReader()) {
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        oldTreeIter.reset(reader, oldHead);
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        newTreeIter.reset(reader, head);

        List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();
        addToGitDiffResult(diffs, diffResult, headCommitId, gitConfig, repository);
      }
    } catch (IOException | GitAPIException ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", "Error in getting commit diff");
    }
    return diffResult;
  }

  private void addToGitDiffResult(List<DiffEntry> diffs, GitDiffResult diffResult, ObjectId headCommitId,
      GitConfig gitConfig, Repository repository) throws IOException {
    logger.info(GIT_YAML_LOG_PREFIX + "Total diff entries found : " + diffs.size());
    for (DiffEntry entry : diffs) {
      String content = null;
      String filePath;
      ObjectId objectId;
      if (entry.getChangeType().equals(DiffEntry.ChangeType.DELETE)) {
        filePath = entry.getOldPath();
        // we still want to collect content for deleted file, as it will be needed to decide yamlhandlerSubType in
        // many cases. so getting oldObjectId
        objectId = entry.getOldId().toObjectId();
      } else {
        filePath = entry.getNewPath();
        objectId = entry.getNewId().toObjectId();
      }
      ObjectLoader loader = repository.open(objectId);
      content = new String(loader.getBytes(), Charset.forName("utf-8"));
      GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                        .withCommitId(headCommitId.getName())
                                        .withChangeType(gitClientHelper.getChangeType(entry.getChangeType()))
                                        .withFilePath(filePath)
                                        .withFileContent(content)
                                        .withObjectId(objectId.name())
                                        .withAccountId(gitConfig.getAccountId())
                                        .build();
      diffResult.addChangeFile(gitFileChange);
    }
  }

  @Override
  public synchronized GitCheckoutResult checkout(GitConfig gitConfig) {
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig)))) {
      Ref ref = git.checkout()
                    .setCreateBranch(true)
                    .setName(gitConfig.getBranch())
                    .setUpstreamMode(SetupUpstreamMode.TRACK)
                    .setStartPoint("origin/" + gitConfig.getBranch())
                    .call();
      return GitCheckoutResult.builder().build();
    } catch (RefAlreadyExistsException refExIgnored) {
      logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType())
          + "Reference already exist do nothing."); // TODO:: check gracefully instead of relying on Exception
      return GitCheckoutResult.builder().build();
    } catch (IOException | GitAPIException ex) {
      logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", "Error in getting commit diff");
    }
  }

  @Override
  public synchronized GitCommitResult commit(GitConfig gitConfig, GitCommitRequest gitCommitRequest) {
    ensureRepoLocallyClonedAndUpdated(gitConfig);

    // TODO:: pull latest remote branch??
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig)))) {
      String timestamp = new SimpleDateFormat(COMMIT_TIMESTAMP_FORMAT).format(new java.util.Date());
      StringBuilder commitMessage = new StringBuilder("Harness IO Git Sync. \n");

      String repoDirectory = gitClientHelper.getRepoDirectory(gitConfig);
      gitCommitRequest.getGitFileChanges().forEach(gitFileChange -> {
        String filePath = repoDirectory + "/" + gitFileChange.getFilePath();
        File file = new File(filePath);
        final ChangeType changeType = gitFileChange.getChangeType();
        switch (changeType) {
          case ADD:
          case MODIFY:
            try {
              logger.info(
                  getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Adding git file " + gitFileChange.toString());
              file.getParentFile().mkdirs();
              file.createNewFile();
              try (FileWriter writer = new FileWriter(file)) {
                writer.write(gitFileChange.getFileContent());
              }
              git.add().addFilepattern(".").call();
              //              commitMessage.append(format("%s: %s\n", gitFileChange.getChangeType(),
              //              gitFileChange.getFilePath()));
            } catch (IOException | GitAPIException ex) {
              logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType())
                  + "Exception in adding/modifying file to git " + ex);
              throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR)
                  .addParam("message", "Error in ADD/MODIFY git operation");
            }
            break;
          //          case COPY:
          //            throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR, "message", "Unhandled git operation: " +
          //            gitFileChange.getChangeType());
          case RENAME:
            try {
              logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Old path:[{}], new path: [{}]",
                  gitFileChange.getOldFilePath(), gitFileChange.getFilePath());
              String oldFilePath = repoDirectory + "/" + gitFileChange.getOldFilePath();
              String newFilePath = repoDirectory + "/" + gitFileChange.getFilePath();

              File oldFile = new File(oldFilePath);
              File newFile = new File(newFilePath);

              if (oldFile.exists()) {
                Path path = Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                git.add().addFilepattern(gitFileChange.getFilePath()).call();
                git.rm().addFilepattern(gitFileChange.getOldFilePath()).call();
                //                commitMessage.append(
                //                    format("%s: %s -> %s\n", gitFileChange.getChangeType(),
                //                    gitFileChange.getOldFilePath(), gitFileChange.getFilePath()));
              } else {
                logger.warn(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "File doesn't exist. path: [{}]",
                    gitFileChange.getOldFilePath());
              }
            } catch (IOException | GitAPIException ex) {
              logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception in moving file", ex);
              // TODO:: check before moving and then uncomment this exception
              throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR)
                  .addParam("message", "Error in RENAME git operation");
            }
            break;
          case DELETE:
            try {
              File fileToBeDeleted = new File(repoDirectory + "/" + gitFileChange.getFilePath());
              if (fileToBeDeleted.exists()) {
                logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Deleting git file "
                    + gitFileChange.toString());
                git.rm().addFilepattern(gitFileChange.getFilePath()).call();
                //                commitMessage.append(format("%s: %s\n", gitFileChange.getChangeType(),
                //                gitFileChange.getFilePath()));
              } else {
                logger.warn(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "File already deleted. path: [{}]",
                    gitFileChange.getFilePath());
              }
            } catch (GitAPIException ex) {
              logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception in deleting file" + ex);
              throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR)
                  .addParam("message", "Error in DELETE git operation");
            }
            break;
          default:
            unhandled(changeType);
        }
      });
      Status status = git.status().call();
      if (status.getAdded().isEmpty() && status.getChanged().isEmpty() && status.getRemoved().isEmpty()) {
        logger.warn(
            getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "No git change to commit. GitCommitRequest: [{}]",
            gitCommitRequest);
        return GitCommitResult.builder().build(); // do nothing
      } else {
        status.getAdded().forEach(
            filePath -> commitMessage.append(format("%s: %s\n", DiffEntry.ChangeType.ADD, filePath)));
        status.getChanged().forEach(
            filePath -> commitMessage.append(format("%s: %s\n", DiffEntry.ChangeType.MODIFY, filePath)));
        status.getRemoved().forEach(
            filePath -> commitMessage.append(format("%s: %s\n", DiffEntry.ChangeType.DELETE, filePath)));
      }
      RevCommit revCommit = git.commit()
                                .setCommitter("Harness.io", "support@harness.io")
                                .setAuthor("Harness.io", "support@harness.io")
                                .setAll(true)
                                .setMessage(commitMessage.toString())
                                .call();
      return GitCommitResult.builder().commitId(revCommit.getName()).commitTime(revCommit.getCommitTime()).build();

    } catch (IOException | GitAPIException ex) {
      logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", "Error in writing commit");
    }
  }

  @Override
  public synchronized GitPushResult push(GitConfig gitConfig, boolean forcePush) {
    logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Performing git PUSH, forcePush is: " + forcePush);
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig)))) {
      Iterable<PushResult> pushResults = ((PushCommand) (getAuthConfiguredCommand(git.push(), gitConfig)))
                                             .setRemote("origin")
                                             .setForce(forcePush)
                                             .setRefSpecs(new RefSpec(gitConfig.getBranch()))
                                             .call();

      RemoteRefUpdate remoteRefUpdate = pushResults.iterator().next().getRemoteUpdates().iterator().next();
      RefUpdate refUpdate =
          RefUpdate.builder()
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
        String errorMsg = format("Unable to push changes to git repository. "
                + "Status reported by Remote is: %s and message is: %s. "
                + "Other info: Force push: %s. Fast forward: %s",
            remoteRefUpdate.getStatus(), remoteRefUpdate.getMessage(), remoteRefUpdate.isForceUpdate(),
            remoteRefUpdate.isFastForward());
        throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", errorMsg);
      }
    } catch (IOException | GitAPIException ex) {
      logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
      String errorMsg = Misc.getMessage(ex);
      if (ex instanceof InvalidRemoteException || ex.getCause() instanceof NoRemoteRepositoryException) {
        errorMsg = "Invalid git repo or user doesn't have write access to repository. repo:" + gitConfig.getRepoUrl();
      }

      gitClientHelper.checkIfTransportException(ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", errorMsg);
    }
  }

  @Override
  public synchronized GitCommitAndPushResult commitAndPush(GitConfig gitConfig, GitCommitRequest gitCommitRequest) {
    GitCommitResult commitResult = commit(gitConfig, gitCommitRequest);
    GitCommitAndPushResult gitCommitAndPushResult =
        GitCommitAndPushResult.builder().gitCommitResult(commitResult).build();
    if (isNotBlank(commitResult.getCommitId())) {
      gitCommitAndPushResult.setGitPushResult(push(gitConfig, gitCommitRequest.isForcePush()));
    } else {
      logger.warn(
          getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Null commitId. Nothing to push for request [{}]",
          gitCommitRequest);
    }
    return gitCommitAndPushResult;
  }

  @Override
  public GitFetchFilesResult fetchFilesByPath(GitConfig gitConfig, GitFetchFilesRequest gitRequest) {
    // Default it to yaml
    if (gitConfig.getGitRepoType() == null) {
      gitConfig.setGitRepoType(GitRepositoryType.YAML);
    }

    validateRequiredArgs(gitRequest);

    String gitConnectorId = gitRequest.getGitConnectorId();
    /*
     * ConnectorId is per gitConfig and will result in diff local path for repo
     * */
    synchronized (gitClientHelper.getLockObject(gitConnectorId)) {
      try {
        logger.info(new StringBuilder(128)
                        .append(" Processing Git command: FETCH_FILES ")
                        .append("Account: ")
                        .append(gitConfig.getAccountId())
                        .append(", repo: ")
                        .append(gitConfig.getRepoUrl())
                        .append(gitRequest.isUseBranch() ? ", Branch: " : ", CommitId: ")
                        .append(gitRequest.isUseBranch() ? gitRequest.getBranch() : gitRequest.getCommitId())
                        .append(", filePaths: ")
                        .append(gitRequest.getFilePaths())

                        .toString());

        // create repository/gitFilesDownload/<AccId>/<GitConnectorId> path
        gitClientHelper.createDirStructureForFileDownload(gitConfig, gitConnectorId);

        // clone repo locally without checkout
        String branch = gitRequest.isUseBranch() ? gitRequest.getBranch() : StringUtils.EMPTY;
        cloneRepoForFilePathCheckout(gitConfig, branch, gitConnectorId);

        // if useBranch is set, use it to checkout latest, else checkout given commitId
        if (gitRequest.isUseBranch()) {
          checkoutBranchForPath(gitRequest.getBranch(), gitRequest.getFilePaths(), gitConfig, gitConnectorId);
        } else {
          checkoutGivenCommitForPath(gitRequest.getCommitId(), gitRequest.getFilePaths(), gitConfig, gitConnectorId);
        }

        List<GitFile> gitFiles = new ArrayList<>();
        String repoPath = gitClientHelper.getRepoPathForFileDownload(gitConfig, gitConnectorId);

        gitRequest.getFilePaths().forEach(filePath -> {
          try (Stream<Path> paths = Files.walk(Paths.get(repoPath + "/" + filePath))) {
            paths.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains(".git"))
                .forEach(path -> gitClientHelper.addFiles(gitFiles, path));
          } catch (Exception e) {
            throw new WingsException(GENERAL_ERROR,
                new StringBuilder("Unable to checkout files for filePath [")
                    .append(filePath)
                    .append("]")
                    .append(gitRequest.isUseBranch() ? " for Branch: " : " for CommitId: ")
                    .append(gitRequest.isUseBranch() ? gitRequest.getBranch() : gitRequest.getCommitId())
                    .toString(),
                USER, e);
          }
        });

        resetWorkingDir(gitRequest.getFilePaths(), gitConfig, gitRequest.getGitConnectorId());

        if (isNotEmpty(gitFiles)) {
          gitFiles.forEach(gitFile -> logger.info("File fetched : " + gitFile.getFilePath()));
        }
        return GitFetchFilesResult.builder()
            .files(gitFiles)
            .gitCommitResult(GitCommitResult.builder()
                                 .commitId(gitRequest.isUseBranch() ? "latest" : gitRequest.getCommitId())
                                 .build())
            .build();

      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        throw new WingsException(GENERAL_ERROR,
            new StringBuilder()
                .append("Failed while fetching files ")
                .append(gitRequest.isUseBranch() ? "for Branch: " : "for CommitId: ")
                .append(gitRequest.isUseBranch() ? gitRequest.getBranch() : gitRequest.getCommitId())
                .append(", FilePaths: ")
                .append(gitRequest.getFilePaths())
                .toString(),
            e);
      }
    }
  }

  private void validateRequiredArgs(GitFetchFilesRequest gitRequest) {
    // FilePath cant empty as well as (Branch and commitId both cant be empty)
    if (isEmpty(gitRequest.getFilePaths())) {
      throw new WingsException(GENERAL_ERROR, "FilePaths  can not be empty", USER)
          .addParam("message", "FilePaths  can not be empty");
    }

    if (gitRequest.isUseBranch() && isEmpty(gitRequest.getBranch())) {
      throw new WingsException(GENERAL_ERROR, "useBrach was set but branch is not provided", USER)
          .addParam("message", "useBranch was set but branch is not provided");
    }

    if (!gitRequest.isUseBranch() && isEmpty(gitRequest.getCommitId())) {
      throw new WingsException("useBranch was false but CommitId was not provided", USER)
          .addParam("message", "useBranch was false but CommitId was not provided");
    }
  }

  private void checkoutGivenCommitForPath(
      String commitId, List<String> filePaths, GitConfig gitConfig, String gitConnectorId) {
    try (Git git = Git.open(new File(gitClientHelper.getFileDownloadRepoDirectory(gitConfig, gitConnectorId)))) {
      logger.info("Checking out commitId: " + commitId);
      CheckoutCommand checkoutCommand = git.checkout().setStartPoint(commitId).setCreateBranch(false);

      filePaths.forEach(filePath -> checkoutCommand.addPath(filePath));
      checkoutCommand.call();
      logger.info("Successfully Checked out commitId: " + commitId);
    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
      gitClientHelper.checkIfTransportException(ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR)
          .addParam("message", "Error in checking out commit id " + commitId);
    }
  }

  private void checkoutBranchForPath(
      String branch, List<String> filePaths, GitConfig gitConfig, String gitConnectorId) {
    try (Git git = Git.open(new File(gitClientHelper.getFileDownloadRepoDirectory(gitConfig, gitConnectorId)))) {
      logger.info("Checking out Branch: " + branch);
      CheckoutCommand checkoutCommand = git.checkout()
                                            .setCreateBranch(true)
                                            .setStartPoint("origin/" + branch)
                                            .setForce(true)
                                            .setUpstreamMode(SetupUpstreamMode.TRACK)
                                            .setName(branch);
      if (isRootDirectory(filePaths)) {
        checkoutCommand.setAllPaths(true);
      } else {
        filePaths.forEach(filePath -> checkoutCommand.addPath(filePath));
      }
      checkoutCommand.call();
      logger.info("Successfully Checked out Branch: " + branch);
    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
      gitClientHelper.checkIfTransportException(ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR)
          .addParam("message", "Error in checking out Branch " + branch);
    }
  }

  private boolean isRootDirectory(List<String> filePaths) {
    return filePaths.size() == 1 && filePaths.get(0).equals("");
  }

  private void resetWorkingDir(List<String> filePaths, GitConfig gitConfig, String gitConnectorId) {
    try (Git git = Git.open(new File(gitClientHelper.getFileDownloadRepoDirectory(gitConfig, gitConnectorId)))) {
      logger.info("Resetting repo");
      ResetCommand resetCommand = new ResetCommand(git.getRepository()).setMode(ResetType.HARD);
      resetCommand.call();
      logger.info("Resetting repo completed successfully");
    } catch (Exception ex) {
      logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
      gitClientHelper.checkIfTransportException(ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", "Error in resetting repo");
    }
  }

  @Override
  public synchronized PullResult pull(GitConfig gitConfig) {
    ensureRepoLocallyClonedAndUpdated(gitConfig);
    try (Git git = Git.open(new File(gitClientHelper.getRepoDirectory(gitConfig)))) {
      git.branchCreate()
          .setForce(true)
          .setName(gitConfig.getBranch())
          .setStartPoint("origin/" + gitConfig.getBranch())
          .call();
      git.checkout().setName(gitConfig.getBranch()).call();
      return ((PullCommand) (getAuthConfiguredCommand(git.pull(), gitConfig))).call();
    } catch (IOException | GitAPIException ex) {
      logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", "Error in getting commit diff");
    }
  }

  @Override
  public String validate(GitConfig gitConfig, boolean logError) {
    try {
      // Init Git repo
      LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository();
      lsRemoteCommand = (LsRemoteCommand) getAuthConfiguredCommand(lsRemoteCommand, gitConfig);
      Collection<Ref> refs = lsRemoteCommand.setRemote(gitConfig.getRepoUrl()).setHeads(true).setTags(true).call();
      logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Remote branches [{}]", refs);
    } catch (Exception e) {
      if (logError) {
        logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Git validation failed [{}]", e);
      }
      if (e instanceof InvalidRemoteException || e.getCause() instanceof NoRemoteRepositoryException) {
        return "Invalid git repo " + gitConfig.getRepoUrl();
      }

      if (e instanceof org.eclipse.jgit.api.errors.TransportException) {
        org.eclipse.jgit.api.errors.TransportException te = (org.eclipse.jgit.api.errors.TransportException) e;
        Throwable cause = te.getCause();
        if (cause instanceof TransportException) {
          TransportException tee = (TransportException) cause;
          if (tee.getCause() instanceof UnknownHostException) {
            return UNREACHABLE_HOST.getDescription() + gitConfig.getRepoUrl();
          }
        }
      }
      // Any generic error
      return Misc.getMessage(e);
    }
    return null; // no error
  }

  /**
   * Ensure repo locally cloned. This is called before performing any git operation with remote
   *
   * @param gitConfig the git config
   */
  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  public synchronized void cloneRepoForFilePathCheckout(GitConfig gitConfig, String branch, String connectorId) {
    logger.info(new StringBuilder(64)
                    .append(getGitLogMessagePrefix(gitConfig.getGitRepoType()))
                    .append("Cloning repo without checkout for file fetch op, for GitConfig: ")
                    .append(gitConfig.toString())
                    .toString());

    boolean exceptionOccured = false;
    File repoDir = new File(gitClientHelper.getFileDownloadRepoDirectory(gitConfig, connectorId));
    // If repo already exists, update references
    if (repoDir.exists()) {
      try (Git git = Git.open(repoDir)) {
        // Check URL change (ssh, https) and update in .git/config
        updateRemoteOriginInConfig(gitConfig);

        // update ref with latest commits on remote
        FetchResult fetchResult =
            ((FetchCommand) (getAuthConfiguredCommand(git.fetch(), gitConfig))).call(); // fetch all remote references

        logger.info(new StringBuilder()
                        .append(getGitLogMessagePrefix(gitConfig.getGitRepoType()))
                        .append("result fetched: ")
                        .append(fetchResult.toString())
                        .toString());

        return;
      } catch (Exception ex) {
        exceptionOccured = true;
        if (ex instanceof IOException) {
          logger.warn(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Repo doesn't exist locally [repo: {}], {} ",
              gitConfig.getRepoUrl(), ex);
          logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Do a fresh clone");
        } else {
          logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Hard reset failed for branch [{}]",
              gitConfig.getBranch());
          logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
          gitClientHelper.checkIfTransportException(ex);
        }
      } finally {
        if (exceptionOccured) {
          gitClientHelper.releaseLock(gitConfig, gitClientHelper.getRepoPathForFileDownload(gitConfig, connectorId));
        }
      }
    }

    clone(gitConfig, gitClientHelper.getFileDownloadRepoDirectory(gitConfig, connectorId), branch, true);
  }

  /**
   * Ensure repo locally cloned. This is called before performing any git operation with remote
   *
   * @param gitConfig the git config
   */
  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  public synchronized void ensureRepoLocallyClonedAndUpdated(GitConfig gitConfig) {
    File repoDir = new File(gitClientHelper.getRepoDirectory(gitConfig));
    boolean executionFailed = false;
    if (repoDir.exists()) {
      try (Git git = Git.open(repoDir)) {
        // Check URL change (ssh, https) and update in .git/config
        updateRemoteOriginInConfig(gitConfig);
        logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Repo exist. do hard sync with remote branch");

        FetchResult fetchResult =
            ((FetchCommand) (getAuthConfiguredCommand(git.fetch(), gitConfig))).call(); // fetch all remote references
        checkout(gitConfig);
        Ref ref = git.reset().setMode(ResetType.HARD).setRef("refs/remotes/origin/" + gitConfig.getBranch()).call();
        logger.info(
            getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Hard reset done for branch " + gitConfig.getBranch());
        // TODO:: log failed commits queued and being ignored.
        return;
      } catch (Exception ex) {
        executionFailed = true;
        if (ex instanceof IOException) {
          logger.error(
              getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Repo doesn't exist locally [repo: {}], {} ",
              gitConfig.getRepoUrl(), ex);
        } else {
          if (ex instanceof GitAPIException) {
            logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Hard reset failed for branch [{}]",
                gitConfig.getBranch());
            logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
            gitClientHelper.checkIfTransportException(ex);
          }
        }
      } finally {
        if (executionFailed) {
          // ensureRepoLocallyClonedAndUpdated is called before any git op (commitAndPush, diff)
          // This is synchronized on this singleton class object. So if we are inside in this method, there is
          // no other method inside this one at the same time. Also all callers are synchronized as well.
          // Means if we fail due to existing index.lock it has to be orphan lock file
          // and needs to be deleted.
          gitClientHelper.releaseLock(gitConfig, gitClientHelper.getRepoDirectory(gitConfig));
        }
      }
    }

    // We are here, so either repo doesnt exist or we encounter some error while
    // opening/updating repo
    logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Do a fresh clone");
    clone(gitConfig, gitClientHelper.getRepoDirectory(gitConfig), gitConfig.getBranch(), false);
  }

  protected String getGitLogMessagePrefix(GitRepositoryType repositoryType) {
    return repositoryType.equals(GitRepositoryType.TERRAFORM) ? GIT_TERRAFORM_LOG_PREFIX : GIT_YAML_LOG_PREFIX;
  }

  private TransportCommand getAuthConfiguredCommand(TransportCommand gitCommand, GitConfig gitConfig) {
    if (!gitConfig.isKeyAuth()) {
      setHttpAuthCredential(gitCommand, gitConfig);
    } else {
      setSshAuthCredentials(gitCommand, gitConfig);
    }
    return gitCommand;
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  private void setSshAuthCredentials(TransportCommand gitCommand, GitConfig gitConfig) {
    String keyPath = null;
    try {
      String sshKey = new String(((HostConnectionAttributes) gitConfig.getSshSettingAttribute().getValue()).getKey());
      keyPath = getTempSshKeyPath(sshKey);

      SshSessionFactory sshSessionFactory = getSshSessionFactory(keyPath);

      gitCommand.setTransportConfigCallback(transport -> {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(sshSessionFactory);
      });
    } catch (Exception e) {
      if (EmptyPredicate.isNotEmpty(keyPath)) {
        new File(keyPath).delete();
      }
      throw new InvalidRequestException("Error setting SSH credentials", e);
    }
  }

  private void setHttpAuthCredential(TransportCommand gitCommand, GitConfig gitConfig) {
    gitCommand.setCredentialsProvider(getCredentialsProvider(gitConfig));
  }

  private SshSessionFactory getSshSessionFactory(String sshKeyPath) {
    return new JschConfigSessionFactory() {
      @Override
      protected void configure(OpenSshConfig.Host host, Session session) {
        session.setConfig("StrictHostKeyChecking", "no");
      }

      @Override
      protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
        JSch jsch = super.getJSch(hc, fs);
        jsch.removeAllIdentity();
        jsch.addIdentity(sshKeyPath);
        return jsch;
      }

      @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
      @Override
      public void releaseSession(RemoteSession session) {
        super.releaseSession(session);
        (new File(sshKeyPath)).delete(); // TODO: try-catch security exception
      }
    };
  }

  @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"})
  private String getTempSshKeyPath(String sshKey) throws IOException {
    String keyFilePath = TEMP_SSH_KEY_DIR + "/" + UUIDGenerator.generateUuid();
    File keyFile = new File(keyFilePath);

    File sshDirectory = keyFile.getParentFile();
    if (sshDirectory.exists() || !sshDirectory.isDirectory()) {
      sshDirectory.delete();
    }
    sshDirectory.mkdirs();

    try (FileWriter writer = new FileWriter(keyFile)) {
      writer.write(sshKey);
    }
    return keyFilePath;
  }

  private UsernamePasswordCredentialsProviderWithSkipSslVerify getCredentialsProvider(GitConfig gitConfig) {
    return new UsernamePasswordCredentialsProviderWithSkipSslVerify(gitConfig.getUsername(), gitConfig.getPassword());
  }
}
