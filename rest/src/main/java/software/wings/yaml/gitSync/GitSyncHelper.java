package software.wings.yaml.gitSync;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.ServiceYamlResourceService;
import software.wings.service.intfc.yaml.SetupYamlResourceService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;
import software.wings.yaml.gitSync.YamlGitSync.SyncMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GitSyncHelper {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String COMMIT_AUTHOR = "bsollish";
  private final String COMMIT_EMAIL = "bob@harness.io";
  private final String COMMIT_TIMESTAMP_FORMAT = "yyyy.MM.dd.HH.mm.ss";
  private final String YAML_EXTENSION = ".yaml";
  private final String DEFAULT_COMMIT_BRANCH = "origin";
  private final String TEMP_REPO_PREFIX = "sync-repos_";
  private static final String TEMP_KEY_PREFIX = "sync-keys_";

  private SshSessionFactory sshSessionFactory;
  private TransportConfigCallback transportConfigCallback;
  private Git git;

  public GitSyncHelper() {}

  public GitSyncHelper(String passphrase, String sshKeyPath) {
    this.sshSessionFactory = new CustomJschConfigSessionFactory(passphrase, sshKeyPath);
    this.transportConfigCallback = new CustomTransportConfigCallback(this.sshSessionFactory);
  }

  public Git clone(String Uri, File clonePath) {
    // clone the repo
    CloneCommand cloneCommand = Git.cloneRepository();
    cloneCommand.setURI(Uri);
    cloneCommand.setDirectory(clonePath);
    cloneCommand.setTransportConfigCallback(this.transportConfigCallback);

    try {
      this.git = cloneCommand.call();
      return this.git;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public DirCache add(String filePattern) {
    try {
      DirCache dirCache = git.add().addFilepattern(filePattern).call();

      return dirCache;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public RevCommit commit(String author, String email, String message) {
    if (this.git != null) {
      try {
        RevCommit revCommit = this.git.commit().setAuthor(author, email).setMessage(message).call();
        return revCommit;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  public Iterable<PushResult> push(String remote) {
    if (this.git != null) {
      try {
        PushCommand pushCommand = git.push();
        pushCommand.setRemote(remote);
        // pushCommand.setRefSpecs( new RefSpec( "release_2_0_2:release_2_0_2" ) );
        pushCommand.setTransportConfigCallback(transportConfigCallback);
        Iterable<PushResult> pushResults = pushCommand.call();

        return pushResults;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  public void spitOutStatus() {
    if (this.git != null) {
      try {
        Status status = git.status().call();

        logger.info("***********************************");
        logger.info("Added: " + status.getAdded());
        logger.info("Changed: " + status.getChanged());
        logger.info("Conflicting: " + status.getConflicting());
        logger.info("ConflictingStageState: " + status.getConflictingStageState());
        logger.info("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
        logger.info("Missing: " + status.getMissing());
        logger.info("Modified: " + status.getModified());
        logger.info("Removed: " + status.getRemoved());
        logger.info("Untracked: " + status.getUntracked());
        logger.info("UntrackedFolders: " + status.getUntrackedFolders());
        logger.info("HasUncommittedChanges: " + status.hasUncommittedChanges());
        logger.info("UncommittedChanges: " + status.getUncommittedChanges());
        logger.info("IsClean: " + status.isClean());
        logger.info("***********************************");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public File getTempRepoPath(String entityId) {
    try {
      File repoPath = File.createTempFile(TEMP_REPO_PREFIX + entityId, "");
      repoPath.delete();
      repoPath.mkdirs();

      return repoPath;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  /*
  public void writeAndAdd(File repoPath, List<GitSyncFile> gitSyncFiles) {
    for (GitSyncFile gsf : gitSyncFiles) {
      String fileName = gsf.getName();
      String rootPath = gsf.getRootPath();

      // we need a rootPath WITHOUT leading or trailing slashes
      rootPath = cleanRootPath(rootPath);

      File newFile = new File(repoPath + "/" + rootPath, fileName);
      writeToRepoFile(newFile, gsf.getYaml());

      try {
        DirCache dirCache = null;
        if (rootPath != null && !rootPath.isEmpty()) {
          // add new/changed files within the rootPath
          dirCache = this.git.add().addFilepattern(rootPath).call();
        } else {
          // add file by fileName
          dirCache = this.git.add().addFilepattern(fileName).call();
        }

      } catch (GitAPIException e) {
        e.printStackTrace();
      }
    }
  }
  */

  public void compareAndSaveChanges(File repoPath, List<GitSyncFile> gitSyncFiles,
      YamlResourceService yamlResourceService, SetupYamlResourceService setupYamlResourceService,
      AppYamlResourceService appYamlResourceService, ServiceYamlResourceService serviceYamlResourceService) {
    for (GitSyncFile gsf : gitSyncFiles) {
      String fileName = gsf.getName();
      String rootPath = gsf.getRootPath();

      // we need a rootPath WITHOUT leading or trailing slashes
      rootPath = cleanRootPath(rootPath);

      try {
        String yaml = new String(Files.readAllBytes(Paths.get(repoPath + "/" + rootPath + "/" + fileName)));

        if (!yaml.equals(gsf.getYaml())) {
          logger.info("************* CHANGE FOUND: " + yaml);
          logger.info("************* Class: " + gsf.getKlass().getSimpleName());

          String shortClass = gsf.getKlass().getSimpleName();
          String entityId = gsf.getEntityId();
          String accountId = gsf.getAccountId();
          String appId = gsf.getAppId();
          YamlPayload yamlPayload = new YamlPayload(yaml);
          String settingVariableType = "";

          boolean deleteEnabled = false;

          switch (shortClass) {
            case "Account":
              setupYamlResourceService.updateSetup(accountId, yamlPayload, deleteEnabled);
              break;
            case "Application":
              appYamlResourceService.updateApp(appId, yamlPayload, deleteEnabled);
              break;
            case "Service":
              serviceYamlResourceService.updateService(appId, entityId, yamlPayload, deleteEnabled);
              break;
            case "ServiceCommand":
              yamlResourceService.updateServiceCommand(appId, entityId, yamlPayload, deleteEnabled);
              break;
            case "Environment":
              yamlResourceService.updateEnvironment(appId, entityId, yamlPayload, deleteEnabled);
              break;
            case "Workflow":
              // TODO - updateWorkflow needs to be added
              // yamlResourceService.updateWorkflow(appId, entityId, yamlPayload, deleteEnabled);
              break;
            case "Pipeline":
              yamlResourceService.updatePipeline(appId, entityId, yamlPayload, deleteEnabled);
              break;
            case "Trigger":
              yamlResourceService.updateTrigger(appId, entityId, yamlPayload, deleteEnabled);
              break;
            case "SettingAttribute":
              yamlResourceService.updateSettingAttribute(
                  accountId, entityId, settingVariableType, yamlPayload, deleteEnabled);
              break;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void writeAddCommitPush(YamlGitSync ygs, File repoPath, List<GitSyncFile> gitSyncFiles) {
    String timestamp = new SimpleDateFormat(COMMIT_TIMESTAMP_FORMAT).format(new java.util.Date());
    StringBuilder commitMessage = new StringBuilder("Harness IO Git Sync (" + timestamp + ")");

    if (gitSyncFiles == null) {
      return;
    }

    if (ygs.getSyncMode() == SyncMode.HARNESS_TO_GIT || ygs.getSyncMode() == SyncMode.BOTH) {
      for (GitSyncFile gsf : gitSyncFiles) {
        String name = gsf.getName();
        String yaml = gsf.getYaml();
        SourceType sourceType = gsf.getSourceType();
        Class klass = gsf.getKlass();
        String rootPath = gsf.getRootPath();

        switch (klass.getCanonicalName()) {
          // TODO - we may need this (in some form)
        }

        // String fileName = name + YAML_EXTENSION;
        String fileName = name;

        // we need a rootPath WITHOUT leading or trailing slashes
        rootPath = cleanRootPath(rootPath);

        File newFile = new File(repoPath + "/" + rootPath, fileName);
        writeToRepoFile(newFile, yaml);

        try {
          DirCache dirCache = null;
          if (rootPath != null && !rootPath.isEmpty()) {
            // add new/changed files within the rootPath
            dirCache = this.git.add().addFilepattern(rootPath).call();
          } else {
            // add file by fileName
            dirCache = this.git.add().addFilepattern(fileName).call();
          }

        } catch (GitAPIException e) {
          e.printStackTrace();
        }

        commitMessage.append("\n" + sourceType.name() + ": " + klass.getCanonicalName());
      }
    }

    // commit
    RevCommit rev = this.commit(COMMIT_AUTHOR, COMMIT_EMAIL, commitMessage.toString());

    // push the change
    Iterable<PushResult> pushResults = this.push(DEFAULT_COMMIT_BRANCH);

    logger.info("*************** rev.getFullMessage(): " + rev.getFullMessage());

    for (PushResult pr : pushResults) {
      logger.info("*************** pr.getMessages(): " + pr.getMessages());
    }
  }

  public void shutdown() {
    spitOutStatus();

    // close down git

    logger.info("*************** this.git: " + this.git);

    this.git.close();
  }

  public void cleanupTempFiles(File sshKeyPath, File repoPath) {
    try {
      // clean up TEMP files
      sshKeyPath.delete();

      Path cleanupPath = Paths.get(repoPath.getPath());
      Files.walk(cleanupPath, FileVisitOption.FOLLOW_LINKS)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          /* .peek(System.out::println) */
          .forEach(File::delete);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void cleanupTempRepoFiles(File repoPath) {
    try {
      Path cleanupPath = Paths.get(repoPath.getPath());
      Files.walk(cleanupPath, FileVisitOption.FOLLOW_LINKS)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          /* .peek(System.out::println) */
          .forEach(File::delete);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void writeToRepoFile(File newFile, String yaml) {
    try {
      // we need to do this, as per:
      // https://stackoverflow.com/questions/6666303/javas-createnewfile-will-it-also-create-directories
      newFile.getParentFile().mkdirs();
      newFile.createNewFile();
      FileWriter writer = new FileWriter(newFile);
      writer.write(yaml);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // strips off leading and triling slashes
  public String cleanRootPath(String rootPath) {
    // leading:
    rootPath = rootPath.replaceAll("^/+", "");
    // trailing:
    rootPath = rootPath.replaceAll("/+$", "");

    return rootPath;
  }

  // annoying that we need to write the sshKey to a file, because the addIdentity method in createDefaultJSch
  // of the CustomJschConfigSessionFactory requires a path and won't take the key directly!
  public static File getSshKeyPath(String sshKey, String entityId) {
    try {
      File sshKeyPath = File.createTempFile(TEMP_KEY_PREFIX + entityId, "");

      Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
      perms.add(PosixFilePermission.OWNER_READ);
      perms.add(PosixFilePermission.OWNER_WRITE);
      Files.setPosixFilePermissions(Paths.get(sshKeyPath.getAbsolutePath()), perms);

      FileWriter fw = new FileWriter(sshKeyPath);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(sshKey);

      if (bw != null) {
        bw.close();
      }

      if (fw != null) {
        fw.close();
      }

      return sshKeyPath;

    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  public SshSessionFactory getSshSessionFactory() {
    return sshSessionFactory;
  }

  public void setSshSessionFactory(SshSessionFactory sshSessionFactory) {
    this.sshSessionFactory = sshSessionFactory;
  }

  public TransportConfigCallback getTransportConfigCallback() {
    return transportConfigCallback;
  }

  public void setTransportConfigCallback(TransportConfigCallback transportConfigCallback) {
    this.transportConfigCallback = transportConfigCallback;
  }

  public Git getGit() {
    return git;
  }

  public void setGit(Git git) {
    this.git = git;
  }
}
