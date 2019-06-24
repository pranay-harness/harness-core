package software.wings.service.impl.marketplace.gcp;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.marketplace.gcp.GCPUsageReport;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.marketplace.gcp.GCPMarketPlaceService;
import software.wings.service.intfc.marketplace.gcp.GCPUsageReportService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class GCPMarketPlaceServiceImpl implements GCPMarketPlaceService {
  public static final double DEFAULT_PERCENTILE = 95.0D;
  private static final ChronoUnit SYNC_CHRONO_UNIT = ChronoUnit.DAYS;
  private static final long SYNC_INTERVAL = TimeUnit.DAYS.toDays(1);

  @Inject private InstanceStatService statService;
  @Inject private GCPUsageReportService gcpUsageReportService;

  @Override
  public void createUsageReport(String accountId) {
    Instant usageReportStartTime = this.usageReportStartTime(accountId);
    Instant usageReportEndTime = this.usageReportEndTime(accountId);

    if (null != usageReportStartTime && null != usageReportEndTime) {
      logger.info("Usage report time {} {}", usageReportStartTime, usageReportEndTime);
      createGCPUsageReport(accountId, usageReportStartTime, usageReportEndTime);
    }
  }

  private Instant usageReportStartTime(String accountId) {
    Instant usageReportStartTime = this.gcpUsageReportService.fetchLastGCPUsageReportTime(accountId);

    if (null == usageReportStartTime) {
      return Instant.now();
    }

    return usageReportStartTime;
  }

  private Instant usageReportEndTime(String accountId) {
    return this.statService.getLastSnapshotTime(accountId);
  }

  private void createGCPUsageReport(String accountId, Instant startTime, Instant endTime) {
    Preconditions.checkArgument(endTime.isAfter(startTime), "'endTime' timestamp should be after 'startTime'");

    GCPUsageReportTimeProvider gcpUsageReportTimeProvider =
        new GCPUsageReportTimeProvider(startTime, endTime, SYNC_INTERVAL, SYNC_CHRONO_UNIT);

    Instant reportStartTime = startTime;
    while (gcpUsageReportTimeProvider.hasNext()) {
      Instant reportEndTime = gcpUsageReportTimeProvider.next();
      if (null == reportEndTime) {
        throw new IllegalStateException(
            "reportEndTime is null even though hasNext() returned true. Shouldn't be possible");
      }
      int instanceUsage = getGCPInstanceUsage(accountId, reportStartTime, reportEndTime);
      GCPUsageReport gcpUsageReport =
          new GCPUsageReport(accountId, null, reportStartTime, reportEndTime, instanceUsage);
      this.gcpUsageReportService.create(gcpUsageReport);
      // TODO - Send usage data to GCP
      reportStartTime = reportEndTime;
    }
  }

  private int getGCPInstanceUsage(String accountId, Instant startTime, Instant endTime) {
    double percentile = this.statService.percentile(accountId, startTime, endTime, DEFAULT_PERCENTILE);
    logger.info("Usage start time {} end time {} instance count {} ", startTime, endTime, percentile);
    long instanceTime = (endTime.toEpochMilli() - startTime.toEpochMilli()) / 60000;
    return (int) ((int) percentile * instanceTime);
  }
}
