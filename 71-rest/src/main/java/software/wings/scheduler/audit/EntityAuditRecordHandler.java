package software.wings.scheduler.audit;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import software.wings.audit.AuditRecord;
import software.wings.audit.AuditRecord.AuditRecordKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.service.intfc.AuditService;

import java.util.List;

public class EntityAuditRecordHandler implements Handler<AuditRecord> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private AuditService auditService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("EntityAuditRecordProcessor").poolSize(2).interval(ofSeconds(30)).build(),
        EntityAuditRecordHandler.class,
        MongoPersistenceIterator.<AuditRecord>builder()
            .clazz(AuditRecord.class)
            .fieldName(AuditRecordKeys.nextIteration)
            .targetInterval(ofMinutes(30))
            .acceptableNoAlertDelay(ofSeconds(45))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(AuditRecord entity) {
    // if not last with match auditHeaderId, EXIT
    AuditRecord mostRecentAuditRecord = auditService.fetchMostRecentAuditRecord(entity.getAuditHeaderId());
    if (mostRecentAuditRecord != null && mostRecentAuditRecord.getUuid() != null
        && !mostRecentAuditRecord.getUuid().equals(entity.getUuid())) {
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
        auditRecords.stream().map(AuditRecord::getEntityAuditRecord).collect(toList());

    if (isNotEmpty(recordsToBeAdded)) {
      auditService.addEntityAuditRecordsToSet(recordsToBeAdded, entity.getAccountId(), entity.getAuditHeaderId());
      auditService.deleteTempAuditRecords(auditRecords.stream().map(AuditRecord::getUuid).collect(toList()));
    }
  }
}
