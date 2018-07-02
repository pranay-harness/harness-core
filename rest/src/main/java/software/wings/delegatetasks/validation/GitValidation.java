package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static software.wings.beans.ErrorCode.UNREACHABLE_HOST;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 11/6/17.
 */
public class GitValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(GitValidation.class);
  @Inject @Transient private transient GitClient gitClient;
  @Inject @Transient private transient EncryptionService encryptionService;

  public GitValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
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
      return singletonList(DelegateConnectionResult.builder()
                               .criteria(encryptionDetails.get(0).getEncryptionConfig().toString())
                               .validated(false)
                               .build());
    }

    return singletonList(
        DelegateConnectionResult.builder()
            .criteria(gitConfig.getRepoUrl())
            .validated(!startsWith(gitClient.validate(gitConfig, false), UNREACHABLE_HOST.getDescription()))
            .build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(((GitConfig) getParameters()[1]).getRepoUrl());
  }
}
