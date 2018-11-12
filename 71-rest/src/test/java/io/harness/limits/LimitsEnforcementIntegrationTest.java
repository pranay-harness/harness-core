package io.harness.limits;

import static io.harness.limits.ActionType.CREATE_APPLICATION;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.LimitChecker;
import io.harness.persistence.ReadPref;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtil;

import java.util.concurrent.TimeUnit;

public class LimitsEnforcementIntegrationTest extends BaseIntegrationTest {
  @Inject private WingsPersistence dao;
  @Inject private LimitConfigurationService limitConfigSvc;
  @Inject private LimitCheckerFactory limitCheckerFactory;

  private boolean indexesEnsured;

  // namespacing accountId with class name to prevent collision with other tests
  private static final String NAMESPACE = LimitsEnforcementIntegrationTest.class.getSimpleName();
  private static final String ACCOUNT_ID = "acc-id-" + RandomStringUtils.randomAlphanumeric(5) + "-" + NAMESPACE;
  private static final Action ACTION = new Action(ACCOUNT_ID, CREATE_APPLICATION);

  @Before
  public void init() throws Exception {
    this.cleanUp();
    if (!indexesEnsured && !IntegrationTestUtil.isManagerRunning(client)) {
      Datastore ds = dao.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
      ds.ensureIndexes(Counter.class);
      ds.ensureIndexes(ConfiguredLimit.class);
      indexesEnsured = true;
    }
  }

  @After
  public void cleanUp() {
    Datastore ds = dao.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
    ds.delete(ds.createQuery(ConfiguredLimit.class).filter("accountId", ACTION.getAccountId()));
    ds.delete(ds.createQuery(Counter.class).filter("key", ACTION.key()));
  }

  @Test
  public void testLimitEnforcement() {
    // configure limits
    StaticLimit limit = new StaticLimit(0);
    boolean configured = limitConfigSvc.configure(ACTION.getAccountId(), ACTION.getActionType(), limit);
    assertTrue(configured);

    // check limits
    LimitChecker checker = limitCheckerFactory.getInstance(ACTION);

    if (checker.checkAndConsume()) {
      Assert.fail("since limit is zero, checkAndConsume should return false");
    }
  }

  @Test
  public void testRateBasedLimitEnforcement() throws Exception {
    // configure limits
    RateLimit limit = new RateLimit(1, 4, TimeUnit.SECONDS);
    boolean configured = limitConfigSvc.configure(ACTION.getAccountId(), ACTION.getActionType(), limit);
    assertTrue(configured);

    // check limits
    LimitChecker checker = limitCheckerFactory.getInstance(ACTION);
    assertTrue("1 request allowed every 4 seconds. First request should pass.", checker.checkAndConsume());
    assertFalse("1 request allowed every 4 seconds. Second request should fail.", checker.checkAndConsume());

    // wait ~4s
    Thread.sleep(4005);

    assertTrue("Request made after 4 seconds should pass", checker.checkAndConsume());
  }
}
