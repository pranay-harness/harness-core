package io.harness.limits;

import static io.harness.limits.ActionType.CREATE_APPLICATION;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.LimitChecker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.Integration;

@Integration
public class LimitsEnforcementIntegrationTest extends BaseIntegrationTest {
  @Inject private WingsPersistence dao;
  @Inject private LimitConfigurationService limitConfigSvc;
  @Inject private LimitCheckerFactory limitCheckerFactory;

  private boolean indexesEnsured;

  @Before
  public void ensureIndices() {
    if (!indexesEnsured) {
      dao.getDatastore().ensureIndexes(Counter.class);
      dao.getDatastore().ensureIndexes(ConfiguredLimit.class);
      indexesEnsured = true;
    }
  }

  @After
  public void clearCollection() {
    Datastore ds = dao.getDatastore();
    ds.delete(ds.createQuery(ConfiguredLimit.class));
    ds.delete(ds.createQuery(Counter.class));
  }

  @Test
  public void testLimitEnforcement() {
    String accountId = "some-account-id";

    // configure limits
    StaticLimit limit = new StaticLimit(0);
    boolean configured = limitConfigSvc.configure(accountId, CREATE_APPLICATION, limit);
    assertTrue(configured);

    // check limits
    LimitChecker checker = limitCheckerFactory.getInstance(new Action(accountId, CREATE_APPLICATION));
    if (checker.checkAndConsume()) {
      Assert.fail("since limit is zero, checkAndConsume should return false");
    }
  }
}
