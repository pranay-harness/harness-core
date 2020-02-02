package software.wings.beans.infrastructure.instance.stats;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.InstanceStatsSnapshotKeys;
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
      persistence.getDatastore(InstanceStatsSnapshot.class).ensureIndexes(InstanceStatsSnapshot.class);
      indexesEnsured = true;
    }
  }

  @After
  public void clearCollection() {
    val ds = persistence.getDatastore(InstanceStatsSnapshot.class);
    ds.delete(ds.createQuery(InstanceStatsSnapshot.class).filter(InstanceStatsSnapshotKeys.accountId, SOME_ACCOUNT_ID));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  public void testSerialization() {
    val snapshot = getSampleSnapshot();

    val sum = snapshot.getAggregateCounts().stream().mapToInt(AggregateCount::getCount).sum();
    assertThat(snapshot.getTotal()).isEqualTo(sum);

    val id = persistence.save(snapshot);
    assertThat(id).isNotNull();
    val fetchedSnapshot = persistence.get(InstanceStatsSnapshot.class, id);

    assertThat(snapshot).isEqualTo(fetchedSnapshot);
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
