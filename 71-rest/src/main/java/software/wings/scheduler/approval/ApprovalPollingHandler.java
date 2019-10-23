package software.wings.scheduler.approval;

import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.beans.approval.ApprovalPollingJobEntity.ApprovalPollingJobEntityKeys;
import software.wings.scheduler.ShellScriptApprovalService;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.servicenow.ServiceNowService;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ApprovalPollingHandler implements Handler<ApprovalPollingJobEntity> {
  private static final int POOL_SIZE = 5;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private JiraHelperService jiraHelperService;
  @Inject private ServiceNowService serviceNowService;
  @Inject private ShellScriptApprovalService shellScriptApprovalService;

  public void registerIterators() {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
        POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-ApprovalPolling").build());

    PersistenceIterator iterator =
        persistenceIteratorFactory.create(MongoPersistenceIterator.<ApprovalPollingJobEntity>builder()
                                              .clazz(ApprovalPollingJobEntity.class)
                                              .fieldName(ApprovalPollingJobEntityKeys.nextIteration)
                                              .targetInterval(ofMinutes(1))
                                              .acceptableNoAlertDelay(ofMinutes(1))
                                              .executorService(executor)
                                              .semaphore(new Semaphore(POOL_SIZE))
                                              .handler(this)
                                              .schedulingType(REGULAR)
                                              .redistribute(true));

    executor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 10, TimeUnit.SECONDS);
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
