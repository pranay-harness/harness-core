package software.wings.beans.infrastructure.instance.stats;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.persistence.ReadPref;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.instance.stats.InstanceStatServiceImpl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InstanceStatServiceIntegrationTest extends BaseIntegrationTest {
  @Inject private InstanceStatServiceImpl statService;
  @Inject private WingsPersistence persistence;

  // namespacing accountId so that other tests are not impacted by this
  private static final String SOME_ACCOUNT_ID =
      "some-account-id-" + InstanceStatServiceIntegrationTest.class.getSimpleName();

  private boolean indexesEnsured;

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
  public void testSave() {
    val stats = sampleSnapshot();
    val ds = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
    val initialCount = ds.getCount(ds.createQuery(InstanceStatsSnapshot.class));

    val saved = statService.save(stats);
    assertTrue("stats should be saved", saved);

    val finalCount = ds.getCount(ds.createQuery(InstanceStatsSnapshot.class));
    assertEquals("since one item was saved, count should be incremented by one", initialCount + 1, finalCount);
  }

  @Test
  public void testAggregateOverPeriod() {
    val from = Instant.now();

    val times = 10;
    val statsToSave = list(times, this ::sampleSnapshot);

    statsToSave.forEach(it -> {
      val saved = statService.save(it);
      assertTrue("instance stats should be saved", saved);
    });

    val to = Instant.now();

    val accountId = statsToSave.get(0).getAccountId();
    val timelineFromDb = statService.aggregate(accountId, from, to);

    assertEquals(statsToSave.size(), timelineFromDb.size());

    for (int i = 0; i < timelineFromDb.size(); i++) {
      val savedValue = statsToSave.get(i);
      val snapshotFromDb = timelineFromDb.get(i);
      assertEquals("saved accountID should be same as fetched accountId", accountId, snapshotFromDb.getAccountId());
      assertEquals(savedValue, snapshotFromDb);
    }
  }

  private <T> List<T> list(int times, Supplier<T> supplier) {
    List<T> list = new ArrayList<>();
    for (int i = 0; i < times; i++) {
      list.add(supplier.get());
    }

    return list;
  }

  @Test
  public void testPercentile() {
    val from = Instant.now();

    val times = 100;
    val statsToSave = list(times, this ::sampleSnapshot);

    statsToSave.forEach(it -> {
      val saved = statService.save(it);
      assertTrue("instance stats should be saved", saved);
    });

    val to = Instant.now();
    val accountId = statsToSave.get(0).getAccountId();
    double percentile = statService.percentile(accountId, from, to, 95.0);
    double expected =
        statsToSave.stream().map(InstanceStatsSnapshot::getTotal).sorted().collect(Collectors.toList()).get(95);

    assertEquals(expected, percentile, 0.01);
  }

  private InstanceStatsSnapshot sampleSnapshot() {
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    val instant = Instant.now();

    int total = ThreadLocalRandom.current().nextInt(101, 150);
    int count = ThreadLocalRandom.current().nextInt(10, 100);

    val appAggregates = Arrays.asList(
        new InstanceStatsSnapshot.AggregateCount(EntityType.APPLICATION, "some-app", "some-app-id", count),
        new InstanceStatsSnapshot.AggregateCount(EntityType.APPLICATION, "other-app", "other-app-id", total - count));

    return new InstanceStatsSnapshot(instant, SOME_ACCOUNT_ID, appAggregates);
  }

  @Test
  public void testGetLastSnapshotTime() {
    val before = Instant.now();
    val stat = sampleSnapshot();

    Instant lastTs = statService.getLastSnapshotTime(stat.getAccountId());
    assertNull("No stats saved, so last timestamp should be null", lastTs);

    val saved = statService.save(stat);
    val after = Instant.now();
    assertTrue("instance stats should be successfully saved", saved);

    lastTs = statService.getLastSnapshotTime(stat.getAccountId());
    assertNotNull("stats saved, so last timestamp should NOT be null", lastTs);

    assertTrue(lastTs.isAfter(before));
    assertTrue(lastTs.isBefore(after));
  }
}
