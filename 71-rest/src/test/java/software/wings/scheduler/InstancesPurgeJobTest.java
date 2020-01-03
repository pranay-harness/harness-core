package software.wings.scheduler;

import static io.harness.rule.OwnerRule.ANKIT;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class InstancesPurgeJobTest extends WingsBaseTest {
  private static final int MAX_INSTANCES = 100;
  private static final int MAX_INSTANCE_STATS = 500;

  private static final int MONTHS_INSTANCES_DATA_TO_GENERATE_EXCLUDING_CURRENT_MONTH = 11;
  private static final int MONTHS_INSTANCE_STATS_DATA_TO_GENERATE_EXCLUDING_CURRENT_MONTH = 11;

  @Inject private WingsPersistence wingsPersistence;

  @Inject @InjectMocks private InstancesPurgeJob job;

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldPurgeCorrectly() {
    generateInstances();
    generateInstanceStats();

    List<Instance> allInstances = wingsPersistence.createQuery(Instance.class).asList();
    List<InstanceStatsSnapshot> allInstanceStats = wingsPersistence.createQuery(InstanceStatsSnapshot.class).asList();

    List<Instance> instancesThatShouldBeRetained =
        allInstances.stream()
            .filter(instance
                -> !instance.isDeleted()
                    || instance.getDeletedAt() >= job.getStartingInstantOfRetentionOfInstances().toEpochMilli())
            .collect(toList());

    List<InstanceStatsSnapshot> instanceStatsThatShouldBeRetained =
        allInstanceStats.stream()
            .filter(stat
                -> stat.getTimestamp().toEpochMilli()
                    >= job.getStartingInstantOfRetentionOfInstanceStats().toEpochMilli())
            .collect(toList());

    job.purge();

    List<Instance> retainedInstances = wingsPersistence.createQuery(Instance.class).asList();
    List<InstanceStatsSnapshot> retainedInstanceStats =
        wingsPersistence.createQuery(InstanceStatsSnapshot.class).asList();

    assertThat(retainedInstances).isEqualTo(instancesThatShouldBeRetained);
    assertThat(retainedInstanceStats).isEqualTo(instanceStatsThatShouldBeRetained);
  }

  private void generateInstances() {
    Instant startInstant = LocalDate.now(ZoneOffset.UTC)
                               .minusMonths(MONTHS_INSTANCES_DATA_TO_GENERATE_EXCLUDING_CURRENT_MONTH)
                               .with(TemporalAdjusters.firstDayOfMonth())
                               .atStartOfDay()
                               .toInstant(ZoneOffset.UTC);
    Instant endInstant = Instant.now();

    List<Instance> instances = Stream.generate(() -> createInstanceInTimeRange(startInstant, endInstant))
                                   .limit(MAX_INSTANCES)
                                   .collect(toList());
    wingsPersistence.save(instances);
  }

  private Instance createInstanceInTimeRange(Instant startInstant, Instant endInstant) {
    InstanceBuilder builder = Instance.builder().accountId("DummyAccountId").appId("DummyAppId");
    if (ThreadLocalRandom.current().nextBoolean()) {
      builder.isDeleted(true).deletedAt(
          ThreadLocalRandom.current().nextLong(startInstant.toEpochMilli(), endInstant.toEpochMilli()));
    } else {
      builder.isDeleted(false);
    }
    return builder.build();
  }

  private void generateInstanceStats() {
    Instant startInstant = LocalDate.now(ZoneOffset.UTC)
                               .minusMonths(MONTHS_INSTANCE_STATS_DATA_TO_GENERATE_EXCLUDING_CURRENT_MONTH)
                               .with(TemporalAdjusters.firstDayOfMonth())
                               .atStartOfDay()
                               .toInstant(ZoneOffset.UTC);
    Instant endInstant = Instant.now();

    List<InstanceStatsSnapshot> instanceStats =
        Stream.generate(() -> createInstanceStatInTimeRange(startInstant, endInstant))
            .limit(MAX_INSTANCE_STATS)
            .collect(toList());

    wingsPersistence.save(instanceStats);
  }

  private InstanceStatsSnapshot createInstanceStatInTimeRange(Instant startInstant, Instant endInstant) {
    return new InstanceStatsSnapshot(Instant.ofEpochMilli(ThreadLocalRandom.current().nextLong(
                                         startInstant.toEpochMilli(), endInstant.toEpochMilli())),
        "DummyAccountId", Collections.emptyList());
  }
}
