package software.wings.beans.infrastructure.instance.stats;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import io.harness.persistence.ReadPref;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class InstanceStatsSnapshotIntegrationTest extends BaseIntegrationTest {
  @Inject private WingsPersistence persistence;
  private boolean indexesEnsured;

  // namespacing accountId so that other tests are not impacted by this
  private static final String SOME_ACCOUNT_ID =
      "some-account-id-" + InstanceStatsSnapshotIntegrationTest.class.getSimpleName();

  @Before
  public void ensureIndices() {
    if (!indexesEnsured) {
      persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL).ensureIndexes(InstanceStatsSnapshot.class);
      indexesEnsured = true;
    }
  }

  @After
  public void clearCollection() {
    val ds = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
    ds.delete(ds.createQuery(InstanceStatsSnapshot.class).filter("accountId", SOME_ACCOUNT_ID));
  }

  @Test
  public void testSerialization() {
    val snapshot = getSampleSnapshot();

    val sum = snapshot.getAggregateCounts().stream().mapToInt(AggregateCount::getCount).sum();
    assertEquals(sum, snapshot.getTotal());

    val id = persistence.save(snapshot);
    assertNotNull(id);
    val fetchedSnapshot = persistence.get(InstanceStatsSnapshot.class, id);

    assertEquals(fetchedSnapshot, snapshot);
  }

  private InstanceStatsSnapshot getSampleSnapshot() {
    val instant = Instant.now();
    val aggregates = Arrays.asList(new InstanceStatsSnapshot.AggregateCount(EntityType.APPLICATION, "some-app",
                                       "some-app-id", ThreadLocalRandom.current().nextInt(10, 100)),
        new InstanceStatsSnapshot.AggregateCount(
            EntityType.APPLICATION, "some-other-app", "other-app-id", ThreadLocalRandom.current().nextInt(10, 100)));

    return new InstanceStatsSnapshot(instant, SOME_ACCOUNT_ID, aggregates);
  }
}
