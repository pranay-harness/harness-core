package software.wings.service.impl.yaml;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.ErrorCode.GENERAL_ERROR;
import static software.wings.beans.ErrorCode.GENERAL_YAML_ERROR;
import static software.wings.beans.yaml.Change.ChangeType.ADD;
import static software.wings.beans.yaml.Change.ChangeType.DELETE;
import static software.wings.beans.yaml.Change.ChangeType.MODIFY;
import static software.wings.beans.yaml.Change.ChangeType.RENAME;
import static software.wings.exception.WingsException.SRE;
import static software.wings.exception.WingsException.USER_ADMIN;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import groovy.lang.Singleton;
import io.harness.filesystem.FileIo;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFile;
import software.wings.exception.WingsException;
import software.wings.utils.Misc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Singleton
public class GitClientHelper {
  private static final Logger logger = LoggerFactory.getLogger(GitClientHelper.class);
  public static final String REPOSITORY = "./repository";
  private static final String GIT_REPO_BASE_DIR = "./repository/${REPO_TYPE}/${ACCOUNT_ID}/${REPO_NAME}";
  public static final String REPOSITORY_GIT_FILE_DOWNLOADS = "./repository/gitFileDownloads";
  public static final String REPOSITORY_GIT_FILE_DOWNLOADS_ACCOUNT = "./repository/gitFileDownloads/{ACCOUNT_ID}";
  public static final String REPOSITORY_GIT_FILE_DOWNLOADS_BASE =
      "./repository/gitFileDownloads/{ACCOUNT_ID}/{CONNECTOR_ID}";
  public static final String REPOSITORY_GIT_FILE_DOWNLOADS_REPO_DIR =
      REPOSITORY_GIT_FILE_DOWNLOADS_BASE + "/{REPO_NAME}";

  private LoadingCache<String, Object> cache = CacheBuilder.newBuilder()
                                                   .maximumSize(2000)
                                                   .expireAfterAccess(1, TimeUnit.HOURS)
                                                   .build(new CacheLoader<String, Object>() {
                                                     @Override
                                                     public Object load(String key) throws Exception {
                                                       return new Object();
                                                     }
                                                   });

  public Object getLockObject(String gitConnectorId) {
    try {
      String uniqueGitConfigString = gitConnectorId;
      return cache.get(uniqueGitConfigString);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, WingsException.USER);
    }
  }

  public String getFileDownloadRepoDirectory(GitConfig gitConfig, String connectorId) {
    String repoName = gitConfig.getRepoUrl()
                          .substring(gitConfig.getRepoUrl().lastIndexOf('/') + 1)
                          .split("\\.")[0]; // TODO:: support more url types and validation
    if (gitConfig.getGitRepoType() == null) {
      logger.error("gitRepoType can not be null. defaulting it to YAML");
      gitConfig.setGitRepoType(GitRepositoryType.YAML);
    }
    return REPOSITORY_GIT_FILE_DOWNLOADS_REPO_DIR.replace("{ACCOUNT_ID}", gitConfig.getAccountId())
        .replace("{CONNECTOR_ID}", connectorId)
        .replace("{REPO_NAME}", repoName);
  }

  public void createDirStructureForFileDownload(GitConfig gitConfig, String connectorId) {
    try {
      FileIo.createDirectoryIfDoesNotExist(REPOSITORY);
      FileIo.createDirectoryIfDoesNotExist(REPOSITORY_GIT_FILE_DOWNLOADS);

      FileIo.createDirectoryIfDoesNotExist(
          REPOSITORY_GIT_FILE_DOWNLOADS_ACCOUNT.replace("{ACCOUNT_ID}", gitConfig.getAccountId()));

      FileIo.createDirectoryIfDoesNotExist(
          REPOSITORY_GIT_FILE_DOWNLOADS_BASE.replace("{ACCOUNT_ID}", gitConfig.getAccountId())
              .replace("{CONNECTOR_ID}", connectorId));
    } catch (IOException e) {
      logger.error("Failed to created required dir structure for gitFileDownloads", e);
      throw new WingsException(
          ErrorCode.GENERAL_ERROR, "Failed to created required dir structure for gitFileDownloads", SRE);
    }
  }

  public void addFiles(List<GitFile> gitFiles, Path path) {
    if (gitFiles == null || path == null) {
      throw new WingsException(GENERAL_ERROR, "GitFiles arg is null, will cause NPE", SRE);
    }

    StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
    } catch (IOException e) {
      logger.error("Failed to read file Content {}", path.toString());
      throw new WingsException(GENERAL_ERROR, "Failed to read file Content {}", e);
    }

    String filePath = StringUtils.EMPTY;
    Path fileNamePath = path.getFileName();
    if (fileNamePath != null) {
      filePath = fileNamePath.toString();
    }

    gitFiles.add(GitFile.builder().filePath(filePath).fileContent(contentBuilder.toString()).build());
  }

  public String getRepoPathForFileDownload(GitConfig gitConfig, String gitConnectorId) {
    String repoName = gitConfig.getRepoUrl()
                          .substring(gitConfig.getRepoUrl().lastIndexOf('/') + 1)
                          .split("\\.")[0]; // TODO:: support more url types and validation);

    return REPOSITORY_GIT_FILE_DOWNLOADS_REPO_DIR.replace("{ACCOUNT_ID}", gitConfig.getAccountId())
        .replace("{CONNECTOR_ID}", gitConnectorId)
        .replace("{REPO_NAME}", repoName);
  }

  public synchronized void releaseLock(GitConfig gitConfig, String repoPath) {
    try {
      File repoDir = new File(repoPath);
      File file = new File(repoDir.getAbsolutePath() + "/.git/index.lock");
      FileIo.deleteFileIfExists(file.getAbsolutePath());
    } catch (Exception e) {
      logger.error(new StringBuilder(64)
                       .append("Failed to delete index.lock file for account: ")
                       .append(gitConfig.getAccountId())
                       .append(", Repo URL: ")
                       .append(gitConfig.getRepoUrl())
                       .append(", Branch: ")
                       .append(gitConfig.getBranch())
                       .toString());

      throw new WingsException(GENERAL_YAML_ERROR, "GIT_SYNC_ISSUE: Failed to delete index.lock file", SRE, e);
    }
  }

  public void checkIfTransportException(Exception ex) {
    // TransportException is subclass of GitAPIException. This is thrown when there is any issue in connecting to git
    // repo, like invalid authorization and invalid repo
    if (ex instanceof GitAPIException && ex.getCause() instanceof TransportException) {
      throw new WingsException(ErrorCode.GIT_CONNECTION_ERROR + ":" + Misc.getMessage(ex), USER_ADMIN)
          .addParam(ErrorCode.GIT_CONNECTION_ERROR.name(), ErrorCode.GIT_CONNECTION_ERROR);
    }
  }

  public ChangeType getChangeType(DiffEntry.ChangeType gitDiffChangeType) {
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
}
