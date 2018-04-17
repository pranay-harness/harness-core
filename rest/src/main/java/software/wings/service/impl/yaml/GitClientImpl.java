package software.wings.service.impl.yaml;

import static io.harness.govern.Switch.unhandled;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK;
import static org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE;
import static software.wings.beans.ErrorCode.UNREACHABLE_HOST;
import static software.wings.beans.yaml.Change.ChangeType.ADD;
import static software.wings.beans.yaml.Change.ChangeType.DELETE;
import static software.wings.beans.yaml.Change.ChangeType.MODIFY;
import static software.wings.beans.yaml.Change.ChangeType.RENAME;
import static software.wings.beans.yaml.YamlConstants.GIT_TERRAFORM_LOG_PREFIX;
import static software.wings.beans.yaml.YamlConstants.GIT_YAML_LOG_PREFIX;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import groovy.lang.Singleton;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
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
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
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
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitPushResult;
import software.wings.beans.yaml.GitPushResult.RefUpdate;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.exception.WingsException;
import software.wings.service.intfc.yaml.GitClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by anubhaw on 10/16/17.
 */

@Singleton
public class GitClientImpl implements GitClient {
  private static final String GIT_REPO_BASE_DIR = "./repository/${REPO_TYPE}/${ACCOUNT_ID}/${REPO_NAME}";
  private static final String COMMIT_TIMESTAMP_FORMAT = "yyyy.MM.dd.HH.mm.ss";
  /**
   * The constant DEFAULT_BRANCH.
   */
  public static final String DEFAULT_BRANCH = "master";

  private static final Logger logger = LoggerFactory.getLogger(GitClientImpl.class);

  @Override
  public synchronized GitCloneResult clone(GitConfig gitConfig) {
    String gitRepoDirectory = getRepoDirectory(gitConfig);
    logger.info(new StringBuilder()
                    .append(GIT_YAML_LOG_PREFIX)
                    .append("cloning repo, Git repo directory :")
                    .append(gitRepoDirectory)
                    .toString());

    if (!gitConfig.isKeyAuth()) {
      try (Git git = Git.cloneRepository()
                         .setURI(gitConfig.getRepoUrl())
                         .setCredentialsProvider(getCredentialsProvider(gitConfig))
                         .setDirectory(new File(gitRepoDirectory))
                         .setBranch(gitConfig.getBranch())
                         .call()) {
        return GitCloneResult.builder().build();
      } catch (GitAPIException ex) {
        logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
        checkIfTransportException(ex);
        throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", "Error in cloning repo");
      }
    } else {
      CloneCommand cloneCommand = Git.cloneRepository();
      cloneCommand = (CloneCommand) getAuthConfiguredCommand(cloneCommand, gitConfig);
      try (Git git = cloneCommand.setURI(gitConfig.getRepoUrl())
                         .setDirectory(new File(gitRepoDirectory))
                         .setBranch(gitConfig.getBranch())
                         .call()) {
        return GitCloneResult.builder().build();
      } catch (GitAPIException ex) {
        logger.error(GIT_YAML_LOG_PREFIX + "Exception: ", ex);
        checkIfTransportException(ex);
        throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", "Error in cloning repo");
      }
    }
  }

  private TransportCommand getAuthConfiguredCommand(TransportCommand gitCommand, GitConfig gitConfig) {
    // HTTPS Connection
    if (!gitConfig.isKeyAuth()) {
      gitCommand.setCredentialsProvider(getCredentialsProvider(gitConfig));
    } else { // SSH Connection
      SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host host, Session session) {
          session.setConfig("StrictHostKeyChecking", "no");
          SshSessionConfig newConfig =
              aSshSessionConfig()
                  .withKey(((HostConnectionAttributes) gitConfig.getSshSettingAttribute().getValue()).getKey())
                  .withKeyName(gitConfig.getSshSettingId())
                  .withHost(host.getHostName())
                  .withUserName(gitConfig.getUsername())
                  .withPort(host.getPort())
                  .build();
          try {
            session = getSSHSession(newConfig);
          } catch (JSchException jse) {
            logger.error("getAuthConfiguredCommand : Could not get SSH Session", jse);
          }
        }

        // https://stackoverflow.com/questions/13686643/using-keys-with-jgit-to-access-a-git-repository-securely/19931041#19931041
        @Override
        protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
          JSch jsch = super.getJSch(hc, fs);
          jsch.removeAllIdentity();
          return jsch;
        }
      };

      gitCommand.setTransportConfigCallback(transport -> {
        SshTransport sshTransport = (SshTransport) transport;
        sshTransport.setSshSessionFactory(sshSessionFactory);
      });
    }

    return gitCommand;
  }

  private UsernamePasswordCredentialsProviderWithSkipSslVerify getCredentialsProvider(GitConfig gitConfig) {
    return new UsernamePasswordCredentialsProviderWithSkipSslVerify(gitConfig.getUsername(), gitConfig.getPassword());
  }

  @Override
  public String getRepoDirectory(GitConfig gitConfig) {
    String repoName = gitConfig.getRepoUrl()
                          .substring(gitConfig.getRepoUrl().lastIndexOf('/') + 1)
                          .split("\\.")[0]; // TODO:: support more url types and validation
    if (gitConfig.getGitRepoType() == null) {
      logger.error("gitRepoType can not be null. defaulting it to YAML");
      gitConfig.setGitRepoType(GitRepositoryType.YAML);
    }
    return GIT_REPO_BASE_DIR.replace("${REPO_TYPE}", gitConfig.getGitRepoType().name().toLowerCase())
        .replace("${ACCOUNT_ID}", gitConfig.getAccountId())
        .replace("${REPO_NAME}", repoName);
  }

  private ChangeType getChangeType(DiffEntry.ChangeType gitDiffChangeType) {
    switch (gitDiffChangeType) {
      case ADD:
        return ADD;
      case MODIFY:
        return MODIFY;
      case DELETE:
        return DELETE;
      case RENAME:
        return RENAME;
      default:
        unhandled(gitDiffChangeType);
    }
    return null;
  }

  @Override
  public synchronized GitDiffResult diff(GitConfig gitConfig, String startCommitId) {
    ensureRepoLocallyClonedAndUpdated(gitConfig);

    GitDiffResult diffResult = GitDiffResult.builder()
                                   .branch(gitConfig.getBranch())
                                   .repoName(gitConfig.getRepoUrl())
                                   .gitFileChanges(new ArrayList<>())
                                   .build();
    try (Git git = Git.open(new File(getRepoDirectory(gitConfig)))) {
      git.checkout().setName(gitConfig.getBranch()).call();
      getPullCommand(gitConfig, git).call();
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

  private PullCommand getPullCommand(GitConfig gitConfig, Git git) {
    return (PullCommand) (getAuthConfiguredCommand(git.pull(), gitConfig));
  }

  private PushCommand getPushCommand(GitConfig gitConfig, Git git) {
    return (PushCommand) (getAuthConfiguredCommand(git.push(), gitConfig));
  }

  private CloneCommand getCloneCommand(GitConfig gitConfig, Git git) {
    return (CloneCommand) (getAuthConfiguredCommand(Git.cloneRepository(), gitConfig));
  }

  private LsRemoteCommand getLsRemoteCommand(GitConfig gitConfig, Git git) {
    return (LsRemoteCommand) (getAuthConfiguredCommand(git.lsRemote(), gitConfig));
  }

  private FetchCommand getFetchCommand(GitConfig gitConfig, Git git) {
    return (FetchCommand) (getAuthConfiguredCommand(git.fetch(), gitConfig));
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
                                        .withChangeType(getChangeType(entry.getChangeType()))
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
    try (Git git = Git.open(new File(getRepoDirectory(gitConfig)))) {
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
    try (Git git = Git.open(new File(getRepoDirectory(gitConfig)))) {
      String timestamp = new SimpleDateFormat(COMMIT_TIMESTAMP_FORMAT).format(new java.util.Date());
      StringBuilder commitMessage = new StringBuilder("Harness IO Git Sync. \n");

      gitCommitRequest.getGitFileChanges().forEach(gitFileChange -> {
        String repoDirectory = getRepoDirectory(gitConfig);
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
                writer.close();
              }
              git.add().addFilepattern(".").call();
              //              commitMessage.append(String.format("%s: %s\n", gitFileChange.getChangeType(),
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
                //                    String.format("%s: %s -> %s\n", gitFileChange.getChangeType(),
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
                //                commitMessage.append(String.format("%s: %s\n", gitFileChange.getChangeType(),
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
            filePath -> commitMessage.append(String.format("%s: %s\n", DiffEntry.ChangeType.ADD, filePath)));
        status.getChanged().forEach(
            filePath -> commitMessage.append(String.format("%s: %s\n", DiffEntry.ChangeType.MODIFY, filePath)));
        status.getRemoved().forEach(
            filePath -> commitMessage.append(String.format("%s: %s\n", DiffEntry.ChangeType.DELETE, filePath)));
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
    try (Git git = Git.open(new File(getRepoDirectory(gitConfig)))) {
      Iterable<PushResult> pushResults = getPushCommand(gitConfig, git)
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
        StringBuilder builder =
            new StringBuilder("Unable to push changes to git repository. Status reported by Remote is: ")
                .append(remoteRefUpdate.getStatus())
                .append(" and message is: ")
                .append(remoteRefUpdate.getMessage())
                .append(". Other info: Force push: ")
                .append(remoteRefUpdate.isForceUpdate())
                .append("")
                .append(remoteRefUpdate.isFastForward());
        String errorMsg = builder.toString();
        throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", errorMsg);
      }
    } catch (IOException | GitAPIException ex) {
      logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
      String errorMsg = ex.getMessage();
      if (ex instanceof InvalidRemoteException | ex.getCause() instanceof NoRemoteRepositoryException) {
        errorMsg = "Invalid git repo or user doesn't have write access to repository. repo:" + gitConfig.getRepoUrl();
      }
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
  public synchronized PullResult pull(GitConfig gitConfig) {
    ensureRepoLocallyClonedAndUpdated(gitConfig);
    try (Git git = Git.open(new File(getRepoDirectory(gitConfig)))) {
      git.branchCreate()
          .setForce(true)
          .setName(gitConfig.getBranch())
          .setStartPoint("origin/" + gitConfig.getBranch())
          .call();
      git.checkout().setName(gitConfig.getBranch()).call();
      return getPullCommand(gitConfig, git).call();
    } catch (IOException | GitAPIException ex) {
      logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
      throw new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", "Error in getting commit diff");
    }
  }

  @Override
  public String validate(GitConfig gitConfig, boolean logError) {
    try {
      // Init Git repo
      Git git = Git.init().setDirectory(new File(getRepoDirectory(gitConfig))).call();
      Collection<Ref> refs =
          getLsRemoteCommand(gitConfig, git).setRemote(gitConfig.getRepoUrl()).setHeads(true).setTags(true).call();
      logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Remote branches [{}]", refs);
    } catch (Exception e) {
      if (logError) {
        logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Git validation failed [{}]", e);
      }
      if (e instanceof InvalidRemoteException | e.getCause() instanceof NoRemoteRepositoryException) {
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
      return e.getMessage();
    }
    return null; // no error
  }

  /**
   * Ensure repo locally cloned. This is called before performing any git operation with remote
   *
   * @param gitConfig the git config
   */
  public synchronized void ensureRepoLocallyClonedAndUpdated(GitConfig gitConfig) {
    try (Git git = Git.open(new File(getRepoDirectory(gitConfig)))) {
      logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Repo exist. do hard sync with remote branch");
      FetchResult fetchResult = getFetchCommand(gitConfig, git).call(); // fetch all remote references
      checkout(gitConfig);
      Ref ref = git.reset().setMode(ResetType.HARD).setRef("refs/remotes/origin/" + gitConfig.getBranch()).call();
      logger.info(
          getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Hard reset done for branch " + gitConfig.getBranch());
      // TODO:: log failed commits queued and being ignored.
      return;
    } catch (IOException ex) {
      logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Repo doesn't exist locally [repo: {}], {}",
          gitConfig.getRepoUrl(), ex);
    } catch (GitAPIException ex) {
      logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Hard reset failed for branch [{}]",
          gitConfig.getBranch());
      logger.error(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Exception: ", ex);
      checkIfTransportException(ex);
    }

    logger.info(getGitLogMessagePrefix(gitConfig.getGitRepoType()) + "Repo doesn't exist. Do a fresh clone");
    clone(gitConfig);
  }

  private void checkIfTransportException(GitAPIException ex) {
    // TransportException is subclass of GitAPIException. This is thrown when there is any issue in connecting to git
    // repo, like invalid authorization and invalid repo
    if (ex.getCause() instanceof TransportException) {
      throw new WingsException(ErrorCode.GIT_CONNECTION_ERROR + ":" + ex.getMessage(), WingsException.ALERTING)
          .addParam(ErrorCode.GIT_CONNECTION_ERROR.name(), ErrorCode.GIT_CONNECTION_ERROR);
    }
  }

  protected String getGitLogMessagePrefix(GitRepositoryType repositoryType) {
    return repositoryType.equals(GitRepositoryType.TERRAFORM) ? GIT_TERRAFORM_LOG_PREFIX : GIT_YAML_LOG_PREFIX;
  }
}
