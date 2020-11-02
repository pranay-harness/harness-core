package software.wings.scheduler;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.utils.TimeUtils.isWeekend;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.DeletedEntity;
import software.wings.beans.DeletedEntity.DeletedEntityKeys;
import software.wings.beans.DeletedEntity.DeletedEntityType;
import software.wings.helpers.ext.account.DeleteAccountHelper;

@Slf4j
public class DeletedEntityHandler implements Handler<DeletedEntity> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<DeletedEntity> persistenceProvider;
  @Inject private DeleteAccountHelper deleteAccountHelper;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("DeletedEntityIterator")
            .poolSize(2)
            .interval(ofMinutes(1))
            .build(),
        DeletedEntityHandler.class,
        MongoPersistenceIterator.<DeletedEntity, MorphiaFilterExpander<DeletedEntity>>builder()
            .clazz(DeletedEntity.class)
            .fieldName(DeletedEntityKeys.nextIteration)
            .targetInterval(ofHours(12))
            .acceptableNoAlertDelay(ofMinutes(Integer.MAX_VALUE))
            .acceptableExecutionTime(ofSeconds(120))
            .persistenceProvider(persistenceProvider)
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(DeletedEntity entity) {
    // handle deleted entities only on weekend
    if (isWeekend()) {
      DeletedEntityType entityType = entity.getEntityType();
      if (entityType.equals(DeletedEntityType.ACCOUNT)) {
        deleteAccountHelper.handleDeletedAccount(entity);
      } else {
        log.error("Unknown entity type: {}", entityType);
      }
    }
  }
}
