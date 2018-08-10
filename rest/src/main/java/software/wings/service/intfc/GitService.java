package software.wings.service.intfc;

import software.wings.beans.GitConfig;
import software.wings.beans.yaml.GitFetchFilesResult;

import java.util.List;

public interface GitService {
  GitFetchFilesResult fetchFilesByPath(
      GitConfig gitConfig, String connectorId, String commitId, String branch, List<String> filePaths);
}
