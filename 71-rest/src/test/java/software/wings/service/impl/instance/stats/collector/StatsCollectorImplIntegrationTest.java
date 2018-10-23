package software.wings.service.impl.instance.stats.collector;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.persistence.ReadPref;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import software.wings.beans.RestResponse;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.resources.stats.model.InstanceTimeline;
import software.wings.resources.stats.model.InstanceTimeline.DataPoint;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.utils.WingsTestConstants;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * End-to-End test which tests:
 * 1. Instance stats are correctly saved
 * 2. Timeline is correctly fetched.
 *
 */
public class StatsCollectorImplIntegrationTest extends BaseIntegrationTest {
  @Inject private WingsPersistence persistence;
  @Inject private StatsCollectorImpl statsCollector;
  @Inject private InstanceStatService statService;

  private boolean ensureIndices;
  private static final String SOME_APP_NAME =
      "some-app-name-" + StatsCollectorImplIntegrationTest.class.getSimpleName();

  @Before
  public void init() throws Exception {
    super.setUp();
    loginAdminUser();

    if (!ensureIndices) {
      Datastore ds = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
      this.cleanUp();
      ds.ensureIndexes(InstanceStatsSnapshot.class);
      ensureIndices = true;
    }
  }

  @After
  public void cleanUp() {
    Datastore ds = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);
    ds.delete(ds.createQuery(Instance.class)
                  .filter("accountId", WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID)
                  .filter("appName", SOME_APP_NAME));
    ds.delete(ds.createQuery(InstanceStatsSnapshot.class)
                  .filter("accountId", WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID));
  }

  @Test
  public void testCreateStats() throws URISyntaxException {
    Datastore ds = persistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL);

    Instant start = Instant.now();
    String accountId = WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID;

    // setup: save instances
    List<Instance> instances = Arrays.asList(sampleInstance(accountId), sampleInstance(accountId));
    instances.forEach(instance -> wingsPersistence.save(instance));

    // call createStats
    long count = ds.getCount(ds.createQuery(InstanceStatsSnapshot.class).filter("accountId", accountId));
    Instant instant = Instant.now();
    boolean success = statsCollector.createStats(accountId, instant);
    long finalCount = ds.getCount(ds.createQuery(InstanceStatsSnapshot.class).filter("accountId", accountId));
    Instant lastTs = statService.getLastSnapshotTime(accountId);
    assertEquals(instant, lastTs);
    assertEquals("new stat entry should be created in database", count + 1, finalCount);
    assertTrue(success);

    // delete all instances
    ds.delete(ds.createQuery(Instance.class));
    instant = Instant.now();

    // call CreateStats again
    success = statsCollector.createStats(accountId, instant);
    assertTrue(success);
    lastTs = statService.getLastSnapshotTime(accountId);
    assertEquals(instant, lastTs);
    long countAfter = ds.getCount(ds.createQuery(InstanceStatsSnapshot.class).filter("accountId", accountId));
    assertEquals("verify new stat entry created in database", finalCount + 1, countAfter);

    Instant end = Instant.now();

    // now hit rest api to get the timeline
    String url = buildAbsoluteUrl("/api/dash-stats/timeline",
        ImmutableMap.of("accountId", accountId, "fromTsMillis", String.valueOf(start.toEpochMilli()), "toTsMillis",
            String.valueOf(end.toEpochMilli())));

    log().info("Timeline Url: {}", url);
    WebTarget target = client.target(url);

    RestResponse<InstanceTimeline> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<InstanceTimeline>>() {});
    InstanceTimeline timeline = restResponse.getResource();
    assertEquals(
        "number of data points is equal to the number of times createStats is called", 2, timeline.getPoints().size());

    DataPoint firstPoint = timeline.getPoints().get(0);
    assertEquals(instances.size(), firstPoint.getTotal());

    DataPoint secondPoint = timeline.getPoints().get(1);
    assertEquals("all the instances were deleted before second createStats call, so total should be zero", 0,
        secondPoint.getTotal());
  }

  private Instance sampleInstance(String accountId) {
    return Instance.builder()
        .accountId(accountId)
        .serviceId("svc-" + RandomStringUtils.randomAlphanumeric(6))
        .infraMappingId("ifm-" + RandomStringUtils.randomAlphanumeric(6))
        .envId("env-" + RandomStringUtils.randomAlphanumeric(6))
        .envName("envName-" + RandomStringUtils.randomAlphanumeric(6))
        .appId("appId-" + RandomStringUtils.randomAlphanumeric(6))
        .appName(SOME_APP_NAME)
        .build();
  }

  private String buildAbsoluteUrl(String path, Map<String, String> params) throws URISyntaxException {
    try {
      URIBuilder uriBuilder = new URIBuilder();
      String scheme = StringUtils.isBlank(System.getenv().get("BASE_HTTP")) ? "https" : "http";
      uriBuilder.setScheme(scheme);
      uriBuilder.setHost("localhost");
      uriBuilder.setPort(9090);
      uriBuilder.setPath(path);
      if (params != null) {
        params.forEach((name, value) -> uriBuilder.addParameter(name, value.toString()));
      }
      return uriBuilder.build().toString();
    } catch (URISyntaxException uriSyntaxException) {
      logger.error("Either the path or the baseUrl are probably incorrect.");
      throw uriSyntaxException;
    }
  }
}