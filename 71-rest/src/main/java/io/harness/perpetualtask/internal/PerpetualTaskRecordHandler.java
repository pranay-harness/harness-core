package io.harness.perpetualtask.internal;

import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.DelegateTask;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
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
  public static final Long PERPETUAL_TASK_TIMEOUT = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);

  @Inject DelegateService delegateService;
  @Inject PerpetualTaskRecordDao perpetualTaskRecordDao;
  @Inject PerpetualTaskServiceClientRegistry clientRegistry;

  public static class PerpetualTaskRecordExecutor {
    static int POOL_SIZE = 3;

    public static void registerIterators(Injector injector) {
      final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
          POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-PerpetualTaskRecordProcessor").build());
      final PerpetualTaskRecordHandler handler = new PerpetualTaskRecordHandler();
      injector.injectMembers(handler);

      PersistenceIterator iterator =
          MongoPersistenceIterator.<PerpetualTaskRecord>builder()
              .clazz(PerpetualTaskRecord.class)
              .fieldName(PerpetualTaskRecordKeys.nextIteration)
              .targetInterval(ofSeconds(5))
              .acceptableNoAlertDelay(ofSeconds(45))
              .executorService(executor)
              .semaphore(new Semaphore(POOL_SIZE))
              .handler(handler)
              .filterExpander(query -> query.field(PerpetualTaskRecordKeys.delegateId).equal(""))
              .schedulingType(REGULAR)
              .redistribute(true)
              .build();

      injector.injectMembers(iterator);
      executor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 3, TimeUnit.SECONDS);
    }
  }

  @Override
  public void handle(PerpetualTaskRecord taskRecord) {
    PerpetualTaskServiceClient client = clientRegistry.getClient(taskRecord.getPerpetualTaskType());
    DelegateTask validationTask = client.getValidationTask(taskRecord.getClientContext(), taskRecord.getAccountId());
    try {
      AssignmentTaskResponse response = delegateService.executeTask(validationTask);
      String delegateId = response.getDelegateId();
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
