package io.harness.limits;

import static io.harness.limits.ActionType.CREATE_APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.limits.configuration.LimitConfigurationServiceMongo;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.Integration;

import java.util.concurrent.TimeUnit;

@Integration
public class LimitConfigurationServiceMongoIntegrationTest extends BaseIntegrationTest {
  @Inject private LimitConfigurationServiceMongo configuredLimitService;
  @Inject private WingsPersistence dao;

  private boolean indexesEnsured;

  @Before
  public void ensureIndices() {
    if (!indexesEnsured) {
      dao.getDatastore().ensureIndexes(ConfiguredLimit.class);
      indexesEnsured = true;
    }
  }

  @After
  public void clearCollection() {
    Datastore ds = dao.getDatastore();
    ds.delete(ds.createQuery(ConfiguredLimit.class));
  }

  @Test
  public void testSaveAndGet() {
    String accountId = "some-account-id";
    ConfiguredLimit<StaticLimit> cl = new ConfiguredLimit<>(accountId, new StaticLimit(10), CREATE_APPLICATION);
    boolean configured = configuredLimitService.configure(cl.getAccountId(), CREATE_APPLICATION, cl.getLimit());
    assertTrue(configured);

    ConfiguredLimit fetchedValue = configuredLimitService.get(accountId, CREATE_APPLICATION);
    assertEquals(cl, fetchedValue);

    fetchedValue = configuredLimitService.get("non-existent-id", CREATE_APPLICATION);
    assertNull("fetched value should be null for not configured limit", fetchedValue);
  }

  @Test
  public void testUpsert() {
    String accountId = "some-account-id";
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
    String accountId = "some-account-id";

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
