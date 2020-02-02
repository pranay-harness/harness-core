package io.harness.limits;

import static io.harness.limits.ActionType.CREATE_APPLICATION;
import static io.harness.limits.ActionType.DEPLOY;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.ConfiguredLimit.ConfiguredLimitKeys;
import io.harness.limits.Counter.CounterKeys;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.LimitChecker;
import io.harness.rule.Owner;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Datastore;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtils;

import java.util.concurrent.TimeUnit;

public class LimitsEnforcementIntegrationTest extends BaseIntegrationTest {
  @Inject private WingsPersistence dao;
  @Inject private LimitConfigurationService limitConfigSvc;
  @Inject private LimitCheckerFactory limitCheckerFactory;

  private boolean indexesEnsured;

  // namespacing accountId with class name to prevent collision with other tests
  private static final String NAMESPACE = LimitsEnforcementIntegrationTest.class.getSimpleName();
  private static final String ACCOUNT_ID = "acc-id-" + RandomStringUtils.randomAlphanumeric(5) + "-" + NAMESPACE;
  private static final Action CREATE_APP_ACTION = new Action(ACCOUNT_ID, CREATE_APPLICATION);
  private static final Action DEPLOY_ACTION = new Action(ACCOUNT_ID, DEPLOY);

  @Before
  public void init() throws Exception {
    this.cleanUp();
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      dao.getDatastore(Counter.class).ensureIndexes(Counter.class);
      dao.getDatastore(ConfiguredLimit.class).ensureIndexes(ConfiguredLimit.class);
      indexesEnsured = true;
    }
  }

  @After
  public void cleanUp() {
    Datastore clds = dao.getDatastore(ConfiguredLimit.class);
    clds.delete(clds.createQuery(ConfiguredLimit.class)
                    .filter(ConfiguredLimitKeys.accountId, CREATE_APP_ACTION.getAccountId()));

    Datastore cds = dao.getDatastore(Counter.class);
    cds.delete(cds.createQuery(Counter.class).filter(CounterKeys.key, CREATE_APP_ACTION.key()));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  public void testLimitEnforcement() {
    // configure limits
    StaticLimit limit = new StaticLimit(0);
    boolean configured =
        limitConfigSvc.configure(CREATE_APP_ACTION.getAccountId(), CREATE_APP_ACTION.getActionType(), limit);
    assertThat(configured).isTrue();

    // check limits
    LimitChecker checker = limitCheckerFactory.getInstance(CREATE_APP_ACTION);

    if (checker.checkAndConsume()) {
      Assert.fail("since limit is zero, checkAndConsume should return false");
    }
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  public void testRateBasedLimitEnforcement() throws Exception {
    // configure limits
    RateLimit limit = new RateLimit(1, 4, TimeUnit.SECONDS);
    boolean configured = limitConfigSvc.configure(DEPLOY_ACTION.getAccountId(), DEPLOY_ACTION.getActionType(), limit);
    assertThat(configured).isTrue();

    // check limits
    LimitChecker checker = limitCheckerFactory.getInstance(DEPLOY_ACTION);
    assertThat(checker.checkAndConsume()).isTrue();
    assertThat(checker.checkAndConsume()).isFalse();

    // wait ~4s
    Thread.sleep(4005);

    assertThat(checker.checkAndConsume()).isTrue();
  }
}
