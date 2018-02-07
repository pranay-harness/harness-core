package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;
import static software.wings.beans.ErrorCode.UNREACHABLE_HOST;

import com.google.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by anubhaw on 11/6/17.
 */
public class GitValidation extends AbstractDelegateValidateTask {
  @Inject private GitClient gitClient;
  @Inject private EncryptionService encryptionService;

  public GitValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    GitConfig gitConfig = (GitConfig) parameters[1];
    List<EncryptedDataDetail> encryptionDetails = (List<EncryptedDataDetail>) parameters[2];
    encryptionService.decrypt(gitConfig, encryptionDetails);

    return singletonList(
        DelegateConnectionResult.builder()
            .criteria(gitConfig.getRepoUrl())
            .validated(!StringUtils.startsWith(gitClient.validate(gitConfig), UNREACHABLE_HOST.getDescription()))
            .build());
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o -> o instanceof GitConfig)
                             .map(config -> ((GitConfig) config).getRepoUrl())
                             .findFirst()
                             .orElse(null));
  }
}
