package software.wings.service.impl.trigger;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.beans.WorkflowExecution.STATUS_KEY;
import static software.wings.beans.trigger.TriggerExecution.WEBHOOK_EVENT_DETAILS_BRANCH_NAME_KEY;
import static software.wings.beans.trigger.TriggerExecution.WEBHOOK_EVENT_DETAILS_GIT_CONNECTOR_ID_KEY;
import static software.wings.beans.trigger.TriggerExecution.WEBHOOK_EVENT_DETAILS_WEBHOOK_SOURCE_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.waiter.WaitNotifyEngine;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.GitConfig;
import software.wings.beans.TaskType;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.TriggerExecution.Status;
import software.wings.beans.trigger.TriggerExecution.WebhookEventDetails;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.trigger.request.TriggerDeploymentNeededRequest;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.trigger.TriggerExecutionService;
import software.wings.utils.Validator;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class WebhookTriggerProcessor {
  private static final Logger logger = LoggerFactory.getLogger(WebhookTriggerProcessor.class);
  public static final int TRIGGER_TASK_TIMEOUT = 30;

  @Inject private TriggerExecutionService triggerExecutionService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private DelegateService delegateService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;

  public TriggerExecution fetchLastExecutionForContentChanged(Trigger trigger) {
    WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
    return wingsPersistence.createQuery(TriggerExecution.class)
        .filter(TriggerExecution.APP_ID_KEY, trigger.getAppId())
        .filter(TriggerExecution.TRIGGER_ID_KEY, trigger.getUuid())
        .filter(TriggerExecution.WEBHOOK_TOKEN_KEY, trigger.getWebHookToken())
        .filter(TriggerExecution.WORKFLOW_ID_KEY, trigger.getWorkflowId())
        .filter(WEBHOOK_EVENT_DETAILS_BRANCH_NAME_KEY, webHookTriggerCondition.getBranchName())
        .filter(WEBHOOK_EVENT_DETAILS_GIT_CONNECTOR_ID_KEY, webHookTriggerCondition.getGitConnectorId())
        .filter(WEBHOOK_EVENT_DETAILS_WEBHOOK_SOURCE_KEY, webHookTriggerCondition.getWebhookSource().name())
        .field(STATUS_KEY)
        .in(EnumSet.<TriggerExecution.Status>of(Status.RUNNING, Status.SUCCESS))
        .order("-createdAt")
        .get();
  }

  public boolean validateBranchName(Trigger trigger, TriggerExecution triggerExecution) {
    logger.info("Validating branch name for the trigger {}", trigger.getUuid());
    WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) trigger.getCondition();
    WebhookEventDetails webhookEventDetails = triggerExecution.getWebhookEventDetails();
    if (webHookTriggerCondition.getBranchName() == null
        || webHookTriggerCondition.getBranchName().equals(webhookEventDetails.getBranchName())) {
      logger.info("Validating branch name completed for the trigger {}", trigger.getUuid());
      return true;
    }
    String msg =
        String.format("WebHook event branch name [%s] does not match with the trigger condition branch name [%s]",
            webhookEventDetails.getBranchName(), webHookTriggerCondition.getBranchName());
    logger.info(msg);
    throw new InvalidRequestException(msg, WingsException.USER);
  }

  public boolean checkFileContentOptionSelected(Trigger trigger) {
    TriggerCondition condition = trigger.getCondition();
    if (condition instanceof WebHookTriggerCondition) {
      WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) condition;
      return webHookTriggerCondition.isCheckFileContentChanged();
    }
    return false;
  }

  public void initiateTriggerContentChangeDelegateTask(
      Trigger trigger, TriggerExecution prevTriggerExecution, TriggerExecution triggerExecution) {
    triggerExecution.setStatus(Status.RUNNING);

    WebhookEventDetails webhookEventDetails = triggerExecution.getWebhookEventDetails();
    WebhookEventDetails prevWebhookEventDetails = prevTriggerExecution.getWebhookEventDetails();
    // Set Previous Commit Id
    if (webhookEventDetails.getCommitId().equals(prevWebhookEventDetails.getCommitId())) {
      return;
    }
    webhookEventDetails.setPrevCommitId(prevWebhookEventDetails.getCommitId());
    TriggerExecution savedTriggerExecution = triggerExecutionService.save(triggerExecution);

    logger.info("Initiating file content change delegate task request");
    String accountId = appService.getAccountIdByAppId(trigger.getAppId());

    TriggerDeploymentNeededRequest triggerDeploymentNeededRequest = createTriggerDeploymentNeededRequest(accountId,
        trigger.getAppId(), webhookEventDetails.getGitConnectorId(), webhookEventDetails.getCommitId(),
        webhookEventDetails.getPrevCommitId(), webhookEventDetails.getBranchName(), webhookEventDetails.getFilePaths());

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .taskType(TaskType.TRIGGER_TASK.name())
                                    .data(TaskData.builder()
                                              .parameters(new Object[] {triggerDeploymentNeededRequest})
                                              .timeout(TimeUnit.MINUTES.toMillis(TRIGGER_TASK_TIMEOUT))
                                              .build())
                                    .accountId(accountId)
                                    .appId(trigger.getAppId())
                                    .waitId(waitId)
                                    .build();

    waitNotifyEngine.waitForAll(
        new TriggerCallback(accountId, trigger.getAppId(), savedTriggerExecution.getUuid()), waitId);
    delegateService.queueTask(delegateTask);
    logger.info("Issued file content change delegate task request for trigger execution id {}",
        savedTriggerExecution.getUuid());
  }

  private TriggerDeploymentNeededRequest createTriggerDeploymentNeededRequest(@NotEmpty String accountId,
      @NotEmpty String appId, @NotEmpty String gitConnectorId, @NotEmpty String currentCommitId,
      @NotEmpty String oldCommitId, @NotEmpty String branch, List<String> filePaths) {
    GitConfig gitConfig = settingsService.fetchGitConfigFromConnectorId(gitConnectorId);
    Validator.notNullCheck("Git connector was deleted", gitConfig);
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(gitConfig, null, null);

    return TriggerDeploymentNeededRequest.builder()
        .accountId(accountId)
        .appId(appId)
        .gitConnectorId(gitConnectorId)
        .currentCommitId(currentCommitId)
        .oldCommitId(oldCommitId)
        .branch(branch)
        .filePaths(filePaths)
        .gitConfig(gitConfig)
        .encryptionDetails(encryptionDetails)
        .build();
  }
}
