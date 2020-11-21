package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GitConfig;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class GitFetchFilesValidationHelper {
  @Inject private EncryptionService encryptionService;
  @Inject private GitClient gitClient;

  public boolean validateGitConfig(GitConfig gitConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(gitConfig, encryptionDetails, false);
    } catch (Exception e) {
      log.info("Failed to decrypt " + gitConfig, e);
      return false;
    }

    return isEmpty(gitClient.validate(gitConfig));
  }
}
