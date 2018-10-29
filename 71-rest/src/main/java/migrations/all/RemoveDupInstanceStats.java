package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.AllArgsConstructor;
import lombok.Value;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * remove duplicate entries from `instanceStats` collection.
 */
public class RemoveDupInstanceStats implements Migration {
  private static final Logger log = LoggerFactory.getLogger(RemoveDupInstanceStats.class);

  @Inject private WingsPersistence persistence;

  @Value
  @AllArgsConstructor
  private class UniqueKey {
    private Instant timestamp;
    private String accountId;
  }

  @Override
  public void migrate() {
    log.info("Running migration - remove duplicate entries from `instanceStats` collection");

    try (HIterator<InstanceStatsSnapshot> stats =
             new HIterator<>(persistence.createQuery(InstanceStatsSnapshot.class).fetch())) {
      Set<UniqueKey> statsSnapshotSet = new HashSet<>();

      int deleteCount = 0;
      while (stats.hasNext()) {
        InstanceStatsSnapshot stat = stats.next();
        boolean added = statsSnapshotSet.add(new UniqueKey(stat.getTimestamp(), stat.getAccountId()));
        if (!added) {
          persistence.delete(stat);
          log.info("Deleted: {}", stat.getUuid());
          deleteCount++;
        }
      }

      log.info("Finished RemoveDupInstanceStats Migration. Deleted entries: {}", deleteCount);

    } catch (Exception e) {
      log.error("Error running RemoveDupInstanceStats migration. ", e);
    }
  }
}
