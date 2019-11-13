package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GitConfig;
import software.wings.beans.trigger.TriggerCommand.TriggerCommandType;
import software.wings.helpers.ext.trigger.request.TriggerDeploymentNeededRequest;
import software.wings.helpers.ext.trigger.request.TriggerRequest;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class TriggerValidation extends AbstractDelegateValidateTask {
  @Inject private GitClient gitClient;
  @Inject private EncryptionService encryptionService;

  public TriggerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    logger.info(format("Running validation for task %s", delegateTaskId));

    TriggerRequest triggerRequest = (TriggerRequest) (getParameters()[0]);
    TriggerCommandType triggerCommandType = triggerRequest.getTriggerCommandType();
    boolean validated = false;

    try {
      switch (triggerCommandType) {
        case DEPLOYMENT_NEEDED_CHECK:
          validated = validateDeploymentNeeded(triggerRequest);
          break;

        default:
          unhandled(triggerCommandType);
      }
    } catch (Exception ex) {
      logger.error(format("Exception occurred while validating trigger task for account %s, triggerCommandType %s",
          triggerRequest.getAccountId(), triggerCommandType));
    }

    DelegateConnectionResult delegateConnectionResult =
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build();
    return singletonList(delegateConnectionResult);
  }

  @Override
  public List<String> getCriteria() {
    TriggerRequest triggerRequest = (TriggerRequest) (getParameters()[0]);
    TriggerCommandType triggerCommandType = triggerRequest.getTriggerCommandType();

    switch (triggerCommandType) {
      case DEPLOYMENT_NEEDED_CHECK:
        String repoUrl = ((TriggerDeploymentNeededRequest) triggerRequest).getGitConfig().getRepoUrl();
        return singletonList("Trigger: " + triggerCommandType + " RepoUrl: " + repoUrl);

      default:
        unhandled(triggerCommandType);
    }

    return singletonList("Trigger: unhandled command type");
  }

  private boolean validateDeploymentNeeded(TriggerRequest triggerRequest) {
    TriggerDeploymentNeededRequest triggerDeploymentNeededRequest = (TriggerDeploymentNeededRequest) triggerRequest;
    GitConfig gitConfig = triggerDeploymentNeededRequest.getGitConfig();
    List<EncryptedDataDetail> encryptionDetails = triggerDeploymentNeededRequest.getEncryptionDetails();

    try {
      encryptionService.decrypt(gitConfig, encryptionDetails);
    } catch (Exception e) {
      logger.info("Failed to decrypt " + gitConfig, e);
      return false;
    }

    return isEmpty(gitClient.validate(gitConfig));
  }
}
