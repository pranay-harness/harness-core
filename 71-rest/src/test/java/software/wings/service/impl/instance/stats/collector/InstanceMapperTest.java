package software.wings.service.impl.instance.stats.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Suppliers;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InstanceMapperTest extends WingsBaseTest {
  @Value
  @AllArgsConstructor
  static class App {
    private String appId;
    private String appName;
  }

  private Supplier<List<App>> sampleApps = Suppliers.memoize(
      ()
          -> Arrays.asList(new App(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)),
              new App(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5))));

  @Test
  public void testMapping() {
    Instant ts = Instant.now();
    String accountId = "some-account-id";
    InstanceMapper mapper = new InstanceMapper(ts, accountId);

    List<Instance> instances = getSampleInstance(accountId);
    InstanceStatsSnapshot statsSnapshot = mapper.map(instances);

    assertEquals(ts, statsSnapshot.getTimestamp());
    assertEquals(accountId, statsSnapshot.getAccountId());
    assertEquals(instances.size(), statsSnapshot.getTotal());
    assertEquals(2, statsSnapshot.getAggregateCounts().size());
    assertEquals(2,
        statsSnapshot.getAggregateCounts().stream().filter(s -> s.getEntityType() == EntityType.APPLICATION).count());

    List<App> apps = statsSnapshot.getAggregateCounts()
                         .stream()
                         .filter(it -> it.getEntityType() == EntityType.APPLICATION)
                         .map(it -> new App(it.getId(), it.getName()))
                         .collect(Collectors.toList());

    assertTrue("there should be an entry for each app in aggregate counts", apps.containsAll(sampleApps.get()));
    assertEquals("no aggregation by service should be present", 0,
        statsSnapshot.getAggregateCounts().stream().filter(it -> it.getEntityType() == EntityType.SERVICE).count());
  }

  @Test
  public void testMappingWithEmptySet() {
    Instant ts = Instant.now();
    String accountId = "some-account-id";
    InstanceMapper mapper = new InstanceMapper(ts, accountId);

    List<Instance> instances = Collections.emptyList();
    InstanceStatsSnapshot statsSnapshot = mapper.map(instances);

    assertEquals(ts, statsSnapshot.getTimestamp());
    assertEquals(accountId, statsSnapshot.getAccountId());
    assertEquals(instances.size(), statsSnapshot.getTotal());
    assertEquals(0, statsSnapshot.getAggregateCounts().size());
    assertEquals(0,
        statsSnapshot.getAggregateCounts().stream().filter(it -> it.getEntityType() == EntityType.APPLICATION).count());

    List<App> apps = statsSnapshot.getAggregateCounts()
                         .stream()
                         .filter(it -> it.getEntityType() == EntityType.APPLICATION)
                         .map(it -> new App(it.getId(), it.getName()))
                         .collect(Collectors.toList());

    assertTrue(apps.isEmpty());
    assertEquals("no aggregation by service should be present", 0,
        statsSnapshot.getAggregateCounts().stream().filter(it -> it.getEntityType() == EntityType.SERVICE).count());
  }

  private List<Instance> getSampleInstance(String accountId) {
    return sampleApps.get()
        .stream()
        .map(it
            -> Instance.builder()
                   .accountId(accountId)
                   .appId(it.getAppId())
                   .appName(it.getAppName())
                   .serviceId("some-service-id")
                   .serviceName("some-service-name")
                   .build())
        .collect(Collectors.toList());
  }
}
