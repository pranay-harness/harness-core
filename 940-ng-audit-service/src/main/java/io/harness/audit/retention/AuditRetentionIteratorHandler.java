package io.harness.audit.retention;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.api.AuditService;
import io.harness.audit.entities.AuditRetention;
import io.harness.audit.entities.AuditRetention.AuditRetentionKeys;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(PL)
public class AuditRetentionIteratorHandler implements MongoPersistenceIterator.Handler<AuditRetention> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private AuditService auditService;

  @Override
  public void handle(AuditRetention auditRetention) {
    Instant toBeDeletedTillTimestamp =
        Instant.now().minus(auditRetention.getRetentionPeriodInMonths(), ChronoUnit.MONTHS);
    auditService.deleteExpiredAudits(auditRetention.getAccountIdentifier(), toBeDeletedTillTimestamp);
  }

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("OutboxEventIteratorTask")
            .poolSize(3)
            .interval(ofMinutes(2))
            .build(),
        AuditRetention.class,
        MongoPersistenceIterator.<AuditRetention, SpringFilterExpander>builder()
            .clazz(AuditRetention.class)
            .fieldName(AuditRetentionKeys.nextIteration)
            .targetInterval(ofHours(8))
            .acceptableNoAlertDelay(ofHours(12))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }
}
