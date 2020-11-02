package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.limits.Counter;
import io.harness.limits.Counter.CounterKeys;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;

/**
 * @author marklu on 2019-07-10
 */
@Slf4j
public class LimitCounterAccountIdMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Migrating existing limit counters to add accountId field");

    int count = 0;
    try (HIterator<Counter> counters =
             new HIterator<>(wingsPersistence.createQuery(Counter.class, excludeAuthority).fetch())) {
      while (counters.hasNext()) {
        Counter counter = counters.next();
        if (counter.getAccountId() == null) {
          log.info("Updating counter {}", counter.getKey());

          counter.populateAccountIdFromKey();
          String accountId = counter.getAccountId();
          UpdateOperations<Counter> updateOperations = wingsPersistence.createUpdateOperations(Counter.class);
          updateOperations.set(CounterKeys.accountId, accountId);

          Query<Counter> query =
              wingsPersistence.createQuery(Counter.class).field(CounterKeys.key).equal(counter.getKey());
          wingsPersistence.findAndModify(query, updateOperations, WingsPersistence.upsertReturnNewOptions);
          count++;

          log.info("Completed updating counter {} with accountId {}", counter.getKey(), accountId);
        }
      }
    }

    log.info("Updated all {} counters", count);
  }
}
