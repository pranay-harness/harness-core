package io.harness.perpetualtask.internal;

import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.DelegateService;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PerpetualTaskRecordHandler implements Handler<PerpetualTaskRecord> {
  private static final int POOL_SIZE = 3;

  public static final Long PERPETUAL_TASK_TIMEOUT = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject DelegateService delegateService;
  @Inject PerpetualTaskRecordDao perpetualTaskRecordDao;
  @Inject PerpetualTaskServiceClientRegistry clientRegistry;

  public void registerIterators() {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
        POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-PerpetualTaskRecordProcessor").build());

    PersistenceIterator iterator = persistenceIteratorFactory.create(
        MongoPersistenceIterator.<PerpetualTaskRecord>builder()
            .clazz(PerpetualTaskRecord.class)
            .fieldName(PerpetualTaskRecordKeys.nextIteration)
            .targetInterval(ofSeconds(5))
            .acceptableNoAlertDelay(ofSeconds(45))
            .executorService(executor)
            .semaphore(new Semaphore(POOL_SIZE))
            .handler(this)
            .filterExpander(query -> query.field(PerpetualTaskRecordKeys.delegateId).equal(""))
            .schedulingType(REGULAR)
            .redistribute(true));

    executor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 3, TimeUnit.SECONDS);
  }

  @Override
  public void handle(PerpetualTaskRecord taskRecord) {
    PerpetualTaskServiceClient client = clientRegistry.getClient(taskRecord.getPerpetualTaskType());
    DelegateTask validationTask = client.getValidationTask(taskRecord.getClientContext(), taskRecord.getAccountId());
    try {
      DelegateTaskNotifyResponseData response = delegateService.executeTask(validationTask);
      String delegateId = response.getDelegateMetaInfo().getId();
      logger.info("Delegate {} is assigned to the inactive {} perpetual task with id {}.", delegateId,
          taskRecord.getPerpetualTaskType(), taskRecord.getUuid());
      perpetualTaskRecordDao.setDelegateId(taskRecord.getUuid(), delegateId);
    } catch (Exception e) {
      // TODO: add more granular exception handling
      // TODO: add exponential backoff retries
      logger.error("Failed to assign any Delegate to perpetual task {} ", taskRecord.getUuid(), e);
    }
  }
}
