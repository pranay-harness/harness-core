/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.limits.checker;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.rule.Owner;

import software.wings.dl.WingsPersistence;
import software.wings.integration.IntegrationTestBase;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;

@OwnedBy(PL)
public class StaticLimitVicinityCheckerMongoImplIntegrationTest extends IntegrationTestBase {
  private static final String NAMESPACE = StaticLimitVicinityCheckerMongoImplIntegrationTest.class.getSimpleName();

  @Inject private WingsPersistence persistence;

  @Before
  public void init() {
    Query<Counter> query = wingsPersistence.createQuery(Counter.class).field("key").endsWith(NAMESPACE);
    wingsPersistence.delete(query);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
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
