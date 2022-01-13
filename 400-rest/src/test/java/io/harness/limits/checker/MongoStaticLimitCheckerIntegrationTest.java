/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class MongoStaticLimitCheckerIntegrationTest extends IntegrationTestBase {
  @Inject private WingsPersistence persistence;

  private static final String NAMESPACE = MongoStaticLimitCheckerIntegrationTest.class.getSimpleName();
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  @Before
  public void init() {
    this.cleanUp();
  }

  @After
  public void cleanUp() {
    persistence.delete(persistence.createQuery(Counter.class).field("key").startsWith(NAMESPACE));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testCheckAndConsume() {
    String key = NAMESPACE + "-" + RandomStringUtils.randomAlphanumeric(5) + ":" + ActionType.DEPLOY;
    String id = persistence.save(new Counter(key, 0));
    assertThat(id).isNotNull();

    int maxLimit = 10;
    MongoStaticLimitChecker checker = new MongoStaticLimitChecker(new StaticLimit(maxLimit), persistence, key);

    for (int i = 0; i < maxLimit; i++) {
      boolean allowed = checker.checkAndConsume();
      assertThat(allowed).isTrue();
    }

    boolean allowed = checker.checkAndConsume();
    assertThat(allowed).isFalse();

    int decrementTimes = ThreadLocalRandom.current().nextInt(0, maxLimit);
    for (int i = 0; i < decrementTimes; i++) {
      boolean decrement = checker.decrement();
      assertThat(decrement).isTrue();
    }

    for (int i = 0; i < decrementTimes; i++) {
      allowed = checker.checkAndConsume();
      assertThat(allowed).isTrue();
    }
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testCheckAndConsumeConcurrent() throws ExecutionException, InterruptedException {
    String key = NAMESPACE + "-" + RandomStringUtils.randomAlphanumeric(5) + ":" + ActionType.DEPLOY;
    String id = persistence.save(new Counter(key, 0));
    assertThat(id).isNotNull();

    int maxLimit = 10;
    MongoStaticLimitChecker checker = new MongoStaticLimitChecker(new StaticLimit(maxLimit), persistence, key);

    concurrentConsumeCheck(checker, maxLimit);

    boolean allowed = checker.checkAndConsume();
    assertThat(allowed).isFalse();

    List<Future> deleteFutures = new ArrayList<>();
    int decrementTimes = ThreadLocalRandom.current().nextInt(0, maxLimit);
    for (int i = 0; i < decrementTimes; i++) {
      Future f = executorService.submit(() -> {
        boolean decrement = checker.decrement();
        assertThat(decrement).isTrue();
      });

      deleteFutures.add(f);
    }

    for (Future f : deleteFutures) {
      f.get();
    }

    concurrentConsumeCheck(checker, decrementTimes);
  }

  private void concurrentConsumeCheck(MongoStaticLimitChecker checker, int times)
      throws ExecutionException, InterruptedException {
    List<Future> futures = new ArrayList<>();

    for (int i = 0; i < times; i++) {
      Future f = executorService.submit(() -> {
        boolean allowed = checker.checkAndConsume();
        assertThat(allowed).isTrue();
      });

      futures.add(f);
    }

    for (Future f : futures) {
      f.get();
    }
  }
}
