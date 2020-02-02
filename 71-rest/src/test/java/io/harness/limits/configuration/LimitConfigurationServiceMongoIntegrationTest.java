package io.harness.limits.configuration;

import static io.harness.limits.ActionType.CREATE_APPLICATION;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.ConfiguredLimit.ConfiguredLimitKeys;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.rule.Owner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Datastore;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtils;

import java.util.concurrent.TimeUnit;

public class LimitConfigurationServiceMongoIntegrationTest extends BaseIntegrationTest {
  @Inject private LimitConfigurationServiceMongo configuredLimitService;
  @Inject private WingsPersistence dao;

  // namespacing accountId with class name to prevent collision with other tests
  private static final String SOME_ACCOUNT_ID =
      "some-account-id-" + LimitConfigurationServiceMongoIntegrationTest.class.getSimpleName();

  private boolean indexesEnsured;

  @Before
  public void ensureIndices() throws Exception {
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      dao.getDatastore(ConfiguredLimit.class).ensureIndexes(ConfiguredLimit.class);
      indexesEnsured = true;
    }
  }

  @After
  public void clearCollection() {
    Datastore ds = dao.getDatastore(ConfiguredLimit.class);
    ds.delete(ds.createQuery(ConfiguredLimit.class).filter(ConfiguredLimitKeys.accountId, SOME_ACCOUNT_ID));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  public void testSaveAndGet() {
    ConfiguredLimit<StaticLimit> cl = new ConfiguredLimit<>(SOME_ACCOUNT_ID, new StaticLimit(10), CREATE_APPLICATION);
    boolean configured = configuredLimitService.configure(cl.getAccountId(), CREATE_APPLICATION, cl.getLimit());
    assertThat(configured).isTrue();

    ConfiguredLimit fetchedValue = configuredLimitService.get(SOME_ACCOUNT_ID, CREATE_APPLICATION);
    assertThat(fetchedValue).isEqualTo(cl);

    fetchedValue = configuredLimitService.get("non-existent-id", CREATE_APPLICATION);
    assertThat(fetchedValue).isNull();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  public void testUpsert() {
    String accountId = SOME_ACCOUNT_ID;
    boolean configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, new StaticLimit(10));
    assertThat(configured).isTrue();
    ConfiguredLimit fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertThat(fetchedValue.getLimit()).isEqualTo(new StaticLimit(10));

    configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, new StaticLimit(20));
    assertThat(configured).isTrue();
    fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertThat(fetchedValue.getLimit()).isEqualTo(new StaticLimit(20));

    configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, new StaticLimit(30));
    assertThat(configured).isTrue();
    fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertThat(fetchedValue.getLimit()).isEqualTo(new StaticLimit(30));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  public void testUpsertForRateLimits() {
    String accountId = SOME_ACCOUNT_ID;

    RateLimit limit = new RateLimit(10, 2, TimeUnit.MINUTES);

    ConfiguredLimit<RateLimit> cl = new ConfiguredLimit<>(accountId, limit, CREATE_APPLICATION);
    boolean configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, cl.getLimit());
    assertThat(configured).isTrue();
    ConfiguredLimit fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertThat(fetchedValue).isEqualTo(cl);

    limit = new RateLimit(20, 22, TimeUnit.MINUTES);

    cl = new ConfiguredLimit<>(accountId, limit, CREATE_APPLICATION);
    configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, cl.getLimit());
    assertThat(configured).isTrue();
    fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertThat(fetchedValue).isEqualTo(cl);
  }
}