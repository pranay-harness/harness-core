package io.harness.limits.checker.rate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.checker.rate.UsageBucket.UsageBucketKeys;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.lib.LimitChecker;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MongoSlidingWindowRateLimitCheckerIntegrationTest extends BaseIntegrationTest {
  private static final Logger log = LoggerFactory.getLogger(MongoSlidingWindowRateLimitCheckerIntegrationTest.class);

  @Inject private WingsPersistence persistence;

  private boolean indexesEnsured;
  private static final String SOME_ACCOUNT_ID = "acc-id-" + RandomStringUtils.randomAlphanumeric(5)
      + MongoSlidingWindowRateLimitCheckerIntegrationTest.class.getSimpleName();

  private static final Action ACTION = new Action(SOME_ACCOUNT_ID, ActionType.CREATE_APPLICATION);
  private final ExecutorService executors = Executors.newFixedThreadPool(7);

  @Before
  public void init() throws Exception {
    this.cleanUp();
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      persistence.getDatastore(UsageBucket.class).ensureIndexes(UsageBucket.class);
      indexesEnsured = true;
    }
  }

  @After
  public void cleanUp() throws Exception {
    persistence.delete(persistence.createQuery(UsageBucket.class).filter(UsageBucketKeys.key, ACTION.key()));
  }

  @Value
  private static class Request implements Runnable {
    private int i;
    private LimitChecker limitChecker;

    Request(int i, LimitChecker limitChecker) {
      this.i = i;
      this.limitChecker = limitChecker;
    }

    @NonFinal private boolean allowed;

    @Override
    public void run() {
      this.allowed = limitChecker.checkAndConsume();
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testCheckAndConsume() throws Exception {
    int maxReq = 10;
    int durationInMillis = 5000;
    RateLimit limit = new RateLimit(maxReq, durationInMillis, TimeUnit.MILLISECONDS);
    MongoSlidingWindowRateLimitChecker limitChecker =
        new MongoSlidingWindowRateLimitChecker(limit, persistence, ACTION);

    for (int i = 0; i < maxReq; i++) {
      assertThat(limitChecker.checkAndConsume()).isTrue();
    }

    assertFalse("request should be blocked since limit reached", limitChecker.checkAndConsume());

    Thread.sleep(durationInMillis + 5);
    assertThat(limitChecker.checkAndConsume()).isTrue();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testVicinity() throws Exception {
    int maxReq = 10;
    int durationInMillis = 5000;
    RateLimit limit = new RateLimit(maxReq, durationInMillis, TimeUnit.MILLISECONDS);
    MongoSlidingWindowRateLimitChecker limitChecker =
        new MongoSlidingWindowRateLimitChecker(limit, persistence, ACTION);

    double count = 0.8 * maxReq;
    for (int i = 0; i < count; i++) {
      assertThat(limitChecker.checkAndConsume()).isTrue();
    }

    assertThat(limitChecker.crossed(70)).isTrue();
    assertFalse("requests should NOT have crossed 90% limit since we are using 0.8 as multiple above",
        limitChecker.crossed(90));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testCheckAndConsumeConcurrent() throws Exception {
    int maxAllowedReq = 40;
    int durationInMillis = 5000;
    RateLimit limit = new RateLimit(maxAllowedReq, durationInMillis, TimeUnit.MILLISECONDS);
    wingsPersistence.save(new UsageBucket(ACTION.key(), new ArrayList<>()));

    MongoSlidingWindowRateLimitChecker limitChecker =
        new MongoSlidingWindowRateLimitChecker(limit, persistence, ACTION);

    List<Future> futures = new LinkedList<>();
    List<Request> requests = new LinkedList<>();
    int totalRequests = maxAllowedReq + ThreadLocalRandom.current().nextInt(40, 100);

    for (int i = 0; i < totalRequests; i++) {
      Request request = new Request(i, limitChecker);
      Future f = executors.submit(request);
      futures.add(f);
      requests.add(request);
    }

    for (Future f : futures) {
      f.get();
    }

    long allowedCount = requests.stream().filter(request -> request.allowed).count();
    long disallowedCount = requests.stream().filter(request -> !request.allowed).count();

    // verify that rate limit is within 1 percent accuracy of expected count
    assertEquals(allowedCount, maxAllowedReq, totalRequests / 100.0);
    assertEquals(disallowedCount, totalRequests - maxAllowedReq, totalRequests / 100.0);

    assertFalse("request should be blocked since limit reached", limitChecker.checkAndConsume());

    Thread.sleep(durationInMillis + 10);
    boolean allowed = limitChecker.checkAndConsume();
    assertThat(allowed).isTrue();
  }
}
