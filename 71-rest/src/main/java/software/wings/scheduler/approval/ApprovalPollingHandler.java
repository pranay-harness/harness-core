package software.wings.scheduler.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.beans.approval.ApprovalPollingJobEntity.ApprovalPollingJobEntityKeys;
import software.wings.scheduler.ShellScriptApprovalService;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.ApprovalPolingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.servicenow.ServiceNowService;

@OwnedBy(CDC)
@Slf4j
public class ApprovalPollingHandler implements Handler<ApprovalPollingJobEntity> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private JiraHelperService jiraHelperService;
  @Inject private ServiceNowService serviceNowService;
  @Inject private ShellScriptApprovalService shellScriptApprovalService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ApprovalPolingService approvalPolingService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("ApprovalPolling").poolSize(5).interval(ofSeconds(10)).build(),
        ApprovalPollingHandler.class,
        MongoPersistenceIterator.<ApprovalPollingJobEntity>builder()
            .clazz(ApprovalPollingJobEntity.class)
            .fieldName(ApprovalPollingJobEntityKeys.nextIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(ApprovalPollingJobEntity entity) {
    try (AutoLogContext ignore1 = new AccountLogContext(entity.getAccountId(), OVERRIDE_ERROR)) {
      logger.info(
          "Polling Approval status for approval polling job {} approval type {}", entity, entity.getApprovalType());
    }

    String workflowExecutionId = entity.getWorkflowExecutionId();
    String appId = entity.getAppId();
    WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
    if (workflowExecution == null || ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
      approvalPolingService.delete(entity.getApprovalId());
      return;
    }

    switch (entity.getApprovalType()) {
      case JIRA:
        jiraHelperService.handleJiraPolling(entity);
        return;
      case SERVICENOW:
        serviceNowService.handleServiceNowPolling(entity);
        return;
      case SHELL_SCRIPT:
        shellScriptApprovalService.handleShellScriptPolling(entity);
        return;
      default:
        throw new InvalidRequestException(
            "No Polling should be required for approval type: " + entity.getApprovalType());
    }
  }
}
