package io.harness.limits.checker.rate;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.checker.rate.UsageBucket.UsageBucketKeys;
import io.harness.rule.Owner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtils;

import java.util.Arrays;

public class UsageBucketIntegrationTest extends BaseIntegrationTest {
  @Inject private WingsPersistence persistence;

  private boolean indexesEnsured;

  private static final String KEY = "some-id-" + UsageBucketIntegrationTest.class.getSimpleName();

  @Before
  public void ensureIndices() throws Exception {
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      persistence.getDatastore(UsageBucket.class).ensureIndexes(UsageBucket.class);
      indexesEnsured = true;
    }
  }

  @After
  public void cleanUp() throws Exception {
    persistence.getDatastore(UsageBucket.class)
        .delete(persistence.createQuery(UsageBucket.class).filter(UsageBucketKeys.key, KEY));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  public void testSerialization() {
    UsageBucket bucket = new UsageBucket(KEY, Arrays.asList(10L, 11L, 101L, 102L));

    String id = persistence.save(bucket);
    assertThat(id).isNotNull();

    UsageBucket fetchedBucket = persistence.get(UsageBucket.class, id);
    assertThat(fetchedBucket).isEqualTo(bucket);
  }
}
