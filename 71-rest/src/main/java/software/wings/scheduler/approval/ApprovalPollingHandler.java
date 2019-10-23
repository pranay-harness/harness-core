package software.wings.scheduler.approval;

import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.beans.approval.ApprovalPollingJobEntity.ApprovalPollingJobEntityKeys;
import software.wings.scheduler.ShellScriptApprovalService;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.servicenow.ServiceNowService;

@Slf4j
public class ApprovalPollingHandler implements Handler<ApprovalPollingJobEntity> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private JiraHelperService jiraHelperService;
  @Inject private ServiceNowService serviceNowService;
  @Inject private ShellScriptApprovalService shellScriptApprovalService;

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
    logger.info("Polling Approval status for approval polling job {}", entity);

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
        throw new WingsException("No Polling should be required for approval type: " + entity.getApprovalType());
    }
  }
}
