package software.wings.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.GitOperationContext;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Singleton
public class GitUtilsDelegate {
  private static final String USER_DIR_KEY = "user.dir";

  @Inject private EncryptionService encryptionService;
  @Inject private GitClientHelper gitClientHelper;
  @Inject private GitClient gitClient;

  public String getRequestDataFromFile(String path) {
    Path jsonPath = Paths.get(path);
    try {
      List<String> data = Files.readAllLines(jsonPath);
      return String.join("\n", data);
    } catch (IOException ex) {
      throw new WingsException(ErrorCode.GENERAL_ERROR,
          "Could not checkout file at given "
              + "branch/commitId");
    }
  }

  public GitOperationContext cloneRepo(
      GitConfig gitConfig, GitFileConfig gitFileConfig, List<EncryptedDataDetail> sourceRepoEncryptionDetails) {
    GitOperationContext gitOperationContext =
        GitOperationContext.builder().gitConfig(gitConfig).gitConnectorId(gitFileConfig.getConnectorId()).build();
    try {
      encryptionService.decrypt(gitConfig, sourceRepoEncryptionDetails);
      gitClient.ensureRepoLocallyClonedAndUpdated(gitOperationContext);
    } catch (RuntimeException ex) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, "Unable to clone git repo");
    }
    return gitOperationContext;
  }

  public String resolveScriptDirectory(GitOperationContext gitOperationContext, String scriptPath) {
    return Paths
        .get(Paths.get(System.getProperty(USER_DIR_KEY)).toString(),
            gitClientHelper.getRepoDirectory(gitOperationContext), scriptPath)
        .toString();
  }
}
