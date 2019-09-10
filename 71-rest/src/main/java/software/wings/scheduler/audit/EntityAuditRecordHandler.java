package software.wings.scheduler.audit;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import software.wings.audit.AuditRecord;
import software.wings.audit.AuditRecord.AuditRecordKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.service.intfc.AuditService;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class EntityAuditRecordHandler implements Handler<AuditRecord> {
  @Inject private AuditService auditService;

  public static class EntityAuditRecordExecutor {
    static int POOL_SIZE = 2;
    public static void registerIterators(Injector injector) {
      final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
          POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-EntityAuditRecordProcessor").build());

      final EntityAuditRecordHandler handler = new EntityAuditRecordHandler();
      injector.injectMembers(handler);

      PersistenceIterator iterator = MongoPersistenceIterator.<AuditRecord>builder()
                                         .clazz(AuditRecord.class)
                                         .fieldName(AuditRecordKeys.nextIteration)
                                         .targetInterval(ofMinutes(30))
                                         .acceptableDelay(ofSeconds(45))
                                         .executorService(executor)
                                         .semaphore(new Semaphore(POOL_SIZE))
                                         .handler(handler)
                                         .regular(true)
                                         .redistribute(true)
                                         .build();

      injector.injectMembers(iterator);
      executor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 30, TimeUnit.SECONDS);
    }
  }

  @Override
  public void handle(AuditRecord entity) {
    // if not last with match auditHeaderId, EXIT
    AuditRecord mostRecentAuditRecord = auditService.fetchMostRecentAuditRecord(entity.getAuditHeaderId());
    if (!mostRecentAuditRecord.getUuid().equals(entity.getUuid())) {
      // Seems there are more recent records added in "AuditRecord" collection for this AuditHeaderId.
      // existing now, when newest record will be processed, these entityAuditRecords will be copied to actual
      // "AuditHeader" record.
      return;
    }

    List<AuditRecord> auditRecords =
        auditService.fetchEntityAuditRecordsOlderThanGivenTime(entity.getAuditHeaderId(), entity.getCreatedAt());

    if (isEmpty(auditRecords)) {
      return;
    }

    List<EntityAuditRecord> recordsToBeAdded =
        auditRecords.stream().map(auditRecord -> auditRecord.getEntityAuditRecord()).collect(toList());

    if (isNotEmpty(recordsToBeAdded)) {
      auditService.addEntityAuditRecordsToSet(recordsToBeAdded, entity.getAccountId(), entity.getAuditHeaderId());
      auditService.deleteTempAuditRecords(
          auditRecords.stream().map(auditRecord -> auditRecord.getUuid()).collect(toList()));
    }
  }
}
