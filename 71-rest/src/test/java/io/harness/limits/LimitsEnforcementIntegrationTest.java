package io.harness.limits;

import static io.harness.limits.ActionType.CREATE_APPLICATION;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.LimitChecker;
import io.harness.persistence.ReadPref;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtil;

public class LimitsEnforcementIntegrationTest extends BaseIntegrationTest {
  @Inject private WingsPersistence dao;
  @Inject private LimitConfigurationService limitConfigSvc;
  @Inject private LimitCheckerFactory limitCheckerFactory;

  // namespacing accountId with class name to prevent collision with other tests
  private static final String ACCOUNT_ID = "some-account-id-" + LimitsEnforcementIntegrationTest.class.getSimpleName();
  private boolean indexesEnsured;

  @Before
  public void ensureIndices() throws Exception {
    if (!indexesEnsured && !IntegrationTestUtil.isManagerRunning(client)) {
      Datastore ds = dao.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
      ds.ensureIndexes(Counter.class);
      ds.ensureIndexes(ConfiguredLimit.class);
      indexesEnsured = true;
    }
  }

  @After
  public void clearCollection() {
    Datastore ds = dao.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
    ds.delete(ds.createQuery(ConfiguredLimit.class).filter("accountId", ACCOUNT_ID));
    ds.delete(ds.createQuery(Counter.class).field("key").startsWith(ACCOUNT_ID));
  }

  @Test
  public void testLimitEnforcement() {
    // configure limits
    StaticLimit limit = new StaticLimit(0);
    boolean configured = limitConfigSvc.configure(ACCOUNT_ID, CREATE_APPLICATION, limit);
    assertTrue(configured);

    // check limits
    LimitChecker checker = limitCheckerFactory.getInstance(new Action(ACCOUNT_ID, CREATE_APPLICATION));
    if (checker.checkAndConsume()) {
      Assert.fail("since limit is zero, checkAndConsume should return false");
    }
  }
}