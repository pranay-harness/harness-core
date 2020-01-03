package io.harness.limits.checker;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;

public class StaticLimitVicinityCheckerMongoImplIntegrationTest extends BaseIntegrationTest {
  private static final String NAMESPACE = StaticLimitVicinityCheckerMongoImplIntegrationTest.class.getSimpleName();

  @Inject private WingsPersistence persistence;

  @Before
  public void init() {
    Query<Counter> query = wingsPersistence.createQuery(Counter.class).field("key").endsWith(NAMESPACE);
    wingsPersistence.delete(query);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(IntegrationTests.class)
  public void testCrossed() {
    String key = "some-key-" + NAMESPACE + ":" + ActionType.DEPLOY;
    Counter counter = new Counter(key, 10);
    wingsPersistence.save(counter);
    StaticLimitVicinityChecker checker = new StaticLimitVicinityCheckerMongoImpl(new StaticLimit(12), key, persistence);

    // 10/12 => 0.83
    assertThat(checker.hasCrossedPercentLimit(80)).isTrue();
    assertThat(checker.hasCrossedPercentLimit(85)).isFalse();
  }
}
