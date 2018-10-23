package io.harness.limits.configuration;

import static io.harness.limits.ActionType.CREATE_APPLICATION;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.limits.ConfiguredLimit;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.persistence.ReadPref;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtil;

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
    if (!indexesEnsured && !IntegrationTestUtil.isManagerRunning(client)) {
      dao.getDatastore(DEFAULT_STORE, ReadPref.NORMAL).ensureIndexes(ConfiguredLimit.class);
      indexesEnsured = true;
    }
  }

  @After
  public void clearCollection() {
    Datastore ds = dao.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
    ds.delete(ds.createQuery(ConfiguredLimit.class).filter("accountId", SOME_ACCOUNT_ID));
  }

  @Test
  public void testSaveAndGet() {
    ConfiguredLimit<StaticLimit> cl = new ConfiguredLimit<>(SOME_ACCOUNT_ID, new StaticLimit(10), CREATE_APPLICATION);
    boolean configured = configuredLimitService.configure(cl.getAccountId(), CREATE_APPLICATION, cl.getLimit());
    assertTrue(configured);

    ConfiguredLimit fetchedValue = configuredLimitService.get(SOME_ACCOUNT_ID, CREATE_APPLICATION);
    assertEquals(cl, fetchedValue);

    fetchedValue = configuredLimitService.get("non-existent-id", CREATE_APPLICATION);
    assertNull("fetched value should be null for not configured limit", fetchedValue);
  }

  @Test
  public void testUpsert() {
    String accountId = SOME_ACCOUNT_ID;
    boolean configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, new StaticLimit(10));
    assertTrue(configured);
    ConfiguredLimit fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertEquals(new StaticLimit(10), fetchedValue.getLimit());

    configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, new StaticLimit(20));
    assertTrue(configured);
    fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertEquals(new StaticLimit(20), fetchedValue.getLimit());

    configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, new StaticLimit(30));
    assertTrue(configured);
    fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertEquals(new StaticLimit(30), fetchedValue.getLimit());
  }

  @Test
  public void testUpsertForRateLimits() {
    String accountId = SOME_ACCOUNT_ID;

    RateLimit limit = new RateLimit(10, 2, TimeUnit.MINUTES);

    ConfiguredLimit<RateLimit> cl = new ConfiguredLimit<>(accountId, limit, CREATE_APPLICATION);
    boolean configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, cl.getLimit());
    assertTrue(configured);
    ConfiguredLimit fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertEquals(cl, fetchedValue);

    limit = new RateLimit(20, 22, TimeUnit.MINUTES);

    cl = new ConfiguredLimit<>(accountId, limit, CREATE_APPLICATION);
    configured = configuredLimitService.configure(accountId, CREATE_APPLICATION, cl.getLimit());
    assertTrue(configured);
    fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertEquals(cl, fetchedValue);
  }
}