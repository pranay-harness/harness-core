package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;

@Singleton
@Slf4j
public class GitFetchFilesValidationHelper {
  @Inject private EncryptionService encryptionService;
  @Inject private GitClient gitClient;

  public boolean validateGitConfig(GitConfig gitConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(gitConfig, encryptionDetails);
    } catch (Exception e) {
      logger.info("Failed to decrypt " + gitConfig, e);
      return false;
    }

    if (isNotEmpty(gitClient.validate(gitConfig))) {
      return false;
    }
    return true;
  }
}
