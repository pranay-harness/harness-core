package software.wings.integration.yaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;

@Singleton
public class GitIntegrationTestUtil {
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private GitClient gitClient;

  public GitFetchFilesResult fetchFromGitUsingUsingBranch(
      SettingAttribute settingAttribute, String branchName, Logger logger, List<String> filePaths) throws Exception {
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    GitFetchFilesRequest gitFetchFilesRequest = GitFetchFilesRequest.builder()
                                                    .gitConnectorId(settingAttribute.getUuid())
                                                    .branch(branchName)
                                                    .filePaths(filePaths)
                                                    .useBranch(true)
                                                    .build();

    return gitClient.fetchFilesByPath(gitConfig, gitFetchFilesRequest);
  }
}
