package software.wings.service.impl.instance.stats.collector;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.infrastructure.instance.stats.Mapper;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.instance.stats.collector.StatsCollector;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class StatsCollectorImpl implements StatsCollector {
  private static final Logger log = LoggerFactory.getLogger(StatsCollectorImpl.class);

  private static final long SYNC_INTERVAL = TimeUnit.MINUTES.toMinutes(10);

  private InstanceStatService statService;
  private DashboardStatisticsService dashboardStatisticsService;

  @Inject
  public StatsCollectorImpl(DashboardStatisticsService dashboardStatisticsService, InstanceStatService statService,
      WingsPersistence persistence) {
    this.dashboardStatisticsService = dashboardStatisticsService;
    this.statService = statService;
  }

  @Override
  public boolean createStats(String accountId) {
    Instant lastSnapshot = statService.getLastSnapshotTime(accountId);
    if (null == lastSnapshot) {
      return createStats(accountId, alignedWithMinute(Instant.now(), 10));
    }

    SnapshotTimeProvider snapshotTimeProvider = new SnapshotTimeProvider(lastSnapshot, SYNC_INTERVAL);
    boolean ranAtLeastOnce = false;
    while (snapshotTimeProvider.hasNext()) {
      Instant nextTs = snapshotTimeProvider.next();
      boolean success = createStats(accountId, nextTs);
      ranAtLeastOnce = ranAtLeastOnce || success;
    }

    return ranAtLeastOnce;
  }

  // see tests for behaviour
  static Instant alignedWithMinute(Instant instant, int minuteToTruncateTo) {
    if (LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getMinute() % minuteToTruncateTo == 0) {
      return instant.truncatedTo(ChronoUnit.MINUTES);
    }

    Instant value = instant.truncatedTo(ChronoUnit.HOURS);
    while (!value.plus(minuteToTruncateTo, ChronoUnit.MINUTES).isAfter(instant)) {
      value = value.plus(minuteToTruncateTo, ChronoUnit.MINUTES);
    }

    return value;
  }

  boolean createStats(String accountId, Instant timesamp) {
    try {
      Set<Instance> instances =
          dashboardStatisticsService.getAppInstancesForAccount(accountId, timesamp.toEpochMilli());
      log.info("Fetched instances. Count: {}, Account: {}, Time: {}", instances.size(), accountId, timesamp);

      Mapper<Collection<Instance>, InstanceStatsSnapshot> instanceMapper = new InstanceMapper(timesamp, accountId);
      InstanceStatsSnapshot stats = instanceMapper.map(instances);
      boolean saved = statService.save(stats);

      if (!saved) {
        log.error("Error saving instance usage stats. AccountId: {}, Timestamp: {}", accountId, timesamp);
      }

      return saved;

    } catch (Exception e) {
      log.error("Could not create stats. AccountId: {}", accountId, e);
      return false;
    }
  }
}
