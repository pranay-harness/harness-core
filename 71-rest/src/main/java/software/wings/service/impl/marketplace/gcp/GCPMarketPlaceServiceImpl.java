package software.wings.service.impl.marketplace.gcp;

import com.google.cloudcommerceprocurement.v1.model.Entitlement;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.marketplace.gcp.GcpMarketPlaceConstants;
import io.harness.marketplace.gcp.procurement.GcpProcurementService;
import io.harness.marketplace.gcp.servicecontrol.GCPServiceControlService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.MarketPlace;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.beans.marketplace.gcp.GCPUsageReport;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.marketplace.MarketPlaceService;
import software.wings.service.intfc.marketplace.gcp.GCPMarketPlaceService;
import software.wings.service.intfc.marketplace.gcp.GCPUsageReportService;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class GCPMarketPlaceServiceImpl implements GCPMarketPlaceService {
  public static final double DEFAULT_PERCENTILE = 95.0D;
  private static final ChronoUnit SYNC_CHRONO_UNIT = ChronoUnit.HOURS;
  private static final long SYNC_INTERVAL = TimeUnit.HOURS.toHours(1);

  @Inject private InstanceStatService statService;
  @Inject private GCPUsageReportService gcpUsageReportService;
  @Inject private GCPServiceControlService gcpServiceControlService;
  @Inject private GcpProcurementService gcpProcurementService;
  @Inject private MarketPlaceService marketPlaceService;

  @Override
  public void createUsageReport(String accountId, String gcpAccountId) {
    Optional<Entitlement> entitlementMaybe = fetchActiveEntitlement(gcpAccountId);
    if (entitlementMaybe.isPresent()) {
      Entitlement entitlement = entitlementMaybe.get();
      Instant usageReportStartTime = this.usageReportStartTime(accountId, entitlement);
      Instant usageReportEndTime = this.usageReportEndTime(accountId);

      if (null != usageReportStartTime && null != usageReportEndTime) {
        logger.info("GCP_MKT_PLACE Usage report time {} {}", usageReportStartTime, usageReportEndTime);
        createGCPUsageReport(accountId, entitlement, usageReportStartTime, usageReportEndTime);
      } else {
        logger.warn("GCP_MKT_PLACE start or end time is null : {} {}", usageReportStartTime, usageReportEndTime);
      }

    } else {
      logger.error("GCP_MKT_PLACE No active entitlement present for account {} ", accountId);
    }
  }

  private Instant usageReportStartTime(String accountId, Entitlement entitlement) {
    Instant usageReportStartTime = this.gcpUsageReportService.fetchLastGCPUsageReportTime(accountId);
    Instant entitlementUpdatedTime = fetchEntitlementUpdatedTime(entitlement);

    if (null == usageReportStartTime || entitlementUpdatedTime.isAfter(usageReportStartTime)) {
      logger.info("GCP_MKT_PLACE Entitlement update time {} and usage report time {}", entitlementUpdatedTime,
          usageReportStartTime);
      return entitlementUpdatedTime;
    }
    return usageReportStartTime;
  }

  private Instant fetchEntitlementUpdatedTime(Entitlement entitlement) {
    try {
      return Instant.parse(entitlement.getUpdateTime());
    } catch (DateTimeParseException ex) {
      logger.error("GCP_MKT_PLACE DateTimeParseException ", ex);
    }
    return null;
  }

  private Instant usageReportEndTime(String accountId) {
    return this.statService.getLastSnapshotTime(accountId);
  }

  private void createGCPUsageReport(String accountId, Entitlement entitlement, Instant startTime, Instant endTime) {
    Preconditions.checkArgument(endTime.isAfter(startTime), "'endTime' timestamp should be after 'startTime'");

    logger.info("GCP_MKT_PLACE start time {} and end time {} for execution", startTime, endTime);
    GCPUsageReportTimeProvider gcpUsageReportTimeProvider =
        new GCPUsageReportTimeProvider(startTime, endTime, SYNC_INTERVAL, SYNC_CHRONO_UNIT);

    Instant reportStartTime = startTime;
    while (gcpUsageReportTimeProvider.hasNext()) {
      Instant reportEndTime = gcpUsageReportTimeProvider.next();
      if (null == reportEndTime) {
        throw new IllegalStateException(
            "reportEndTime is null even though hasNext() returned true. Shouldn't be possible");
      }

      long instanceUsage = getGCPInstanceUsage(accountId, reportStartTime, reportEndTime);
      String consumerId = entitlement.getUsageReportingId();
      String entitlementName = entitlement.getName();
      String operationId = String.format("%s-%s", consumerId, reportStartTime.toEpochMilli());
      GCPUsageReport gcpUsageReport = new GCPUsageReport(
          accountId, consumerId, operationId, entitlementName, reportStartTime, reportEndTime, instanceUsage);

      logger.info("GCP_MKT_PLACE Gcp usage report data {} ", gcpUsageReport.toString());

      if (this.gcpServiceControlService.reportUsageDataToGCP(gcpUsageReport)) {
        this.gcpUsageReportService.create(gcpUsageReport);
      } else {
        logger.error("GCP_MKT_PLACE Exception while sending data to GCP, accountId {} start time {} end time {} ",
            accountId, reportStartTime, reportEndTime);
        break;
      }
      reportStartTime = reportEndTime;
    }
  }

  private Optional<Entitlement> fetchActiveEntitlement(String gcpAccountId) {
    Optional<MarketPlace> marketPlaceMaybe =
        this.marketPlaceService.fetchMarketplace(gcpAccountId, MarketPlaceType.GCP);
    if (!marketPlaceMaybe.isPresent()) {
      logger.error("GCP_MKT_PLACE Error marketplace entity not present {} ", gcpAccountId);
      return Optional.empty();
    } else {
      MarketPlace marketPlace = marketPlaceMaybe.get();
      List<Entitlement> entitlements = this.gcpProcurementService.listEntitlements(marketPlace);
      List<Entitlement> activeEntitlement =
          entitlements.stream()
              .filter(entitlement -> entitlement.getState().equals(GcpMarketPlaceConstants.ENTITLEMENT_ACTIVATED))
              .collect(Collectors.toList());
      if (activeEntitlement.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(activeEntitlement.get(0));
    }
  }

  private long getGCPInstanceUsage(String accountId, Instant startTime, Instant endTime) {
    double percentile = this.statService.percentile(accountId, startTime, endTime, DEFAULT_PERCENTILE);
    if (percentile < 0) {
      logger.info("GCP_MKT_PLACE percentile value {}", percentile);
      percentile = 0;
    }
    logger.info("GCP_MKT_PLACE Usage start time {} end time {} instance count {} ", startTime, endTime, percentile);
    long instanceTime = (endTime.toEpochMilli() - startTime.toEpochMilli()) / 60000;
    return (long) (percentile * instanceTime);
  }
}
