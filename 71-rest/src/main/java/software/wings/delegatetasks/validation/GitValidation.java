package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 11/6/17.
 */
@Slf4j
public class GitValidation extends AbstractDelegateValidateTask {
  @Inject private transient GitClient gitClient;
  @Inject private transient EncryptionService encryptionService;

  public GitValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<DelegateConnectionResult> validate() {
    GitConfig gitConfig = (GitConfig) getParameters()[1];
    logger.info("Running validation for task {} for repo {}", delegateTaskId, gitConfig.getRepoUrl());
    List<EncryptedDataDetail> encryptionDetails = (List<EncryptedDataDetail>) getParameters()[2];
    try {
      encryptionService.decrypt(gitConfig, encryptionDetails);
    } catch (Exception e) {
      logger.info("Failed to decrypt " + gitConfig, e);
      return singletonList(DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(false).build());
    }

    boolean validated = true;
    if (isNotEmpty(gitClient.validate(gitConfig))) {
      validated = false;
    }

    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList("GIT:" + ((GitConfig) getParameters()[1]).getRepoUrl());
  }
}
