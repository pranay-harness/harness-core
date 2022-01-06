/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.infrastructure.instance.stats;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.AggregateCount;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot.InstanceStatsSnapshotKeys;
import software.wings.integration.IntegrationTestBase;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InstanceStatsSnapshotIntegrationTest extends IntegrationTestBase {
  @Inject private HPersistence persistence;
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
  @Ignore("skipping the integration test")
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
