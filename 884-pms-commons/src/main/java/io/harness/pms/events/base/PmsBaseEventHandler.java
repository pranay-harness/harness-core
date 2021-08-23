package io.harness.pms.events.base;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.logging.AutoLogContext;
import io.harness.logging.AutoLogContext.OverrideBehavior;
import io.harness.monitoring.EventMonitoringService;
import io.harness.monitoring.MonitoringInfo;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.execution.events.PmsCommonsBaseEventHandler;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public abstract class PmsBaseEventHandler<T extends Message> implements PmsCommonsBaseEventHandler<T> {
  public static String LISTENER_END_METRIC = "%s_queue_time";
  public static String LISTENER_START_METRIC = "%s_time_in_queue";

  @Inject private PmsGitSyncHelper pmsGitSyncHelper;
  @Inject private EventMonitoringService eventMonitoringService;

  protected PmsGitSyncBranchContextGuard gitSyncContext(T event) {
    return pmsGitSyncHelper.createGitSyncBranchContextGuard(extractAmbiance(event), true);
  };

  @NonNull protected abstract Map<String, String> extraLogProperties(T event);

  protected abstract Ambiance extractAmbiance(T event);

  protected abstract Map<String, String> extractMetricContext(T message);

  protected abstract String getMetricPrefix(T message);

  public void handleEvent(T event, Map<String, String> metadataMap, long createdAt) {
    try (PmsGitSyncBranchContextGuard ignore1 = gitSyncContext(event); AutoLogContext ignore2 = autoLogContext(event);
         PmsMetricContextGuard metricContext = new PmsMetricContextGuard(metadataMap, extractMetricContext(event))) {
      log.info("[PMS_MESSAGE_LISTENER] Starting to process {} event ", event.getClass().getSimpleName());
      MonitoringInfo monitoringInfo = MonitoringInfo.builder()
                                          .createdAt(createdAt)
                                          .metricPrefix(getMetricPrefix(event))
                                          .metricContext(metricContext)
                                          .accountId(AmbianceUtils.getAccountId(extractAmbiance(event)))
                                          .build();
      eventMonitoringService.sendMetric(LISTENER_START_METRIC, monitoringInfo, metadataMap);
      handleEventWithContext(event);
      eventMonitoringService.sendMetric(LISTENER_END_METRIC, monitoringInfo, metadataMap);
      log.info(
          "[PMS_MESSAGE_LISTENER] EventHandler processing finished for {} event", event.getClass().getSimpleName());
    } catch (Exception ex) {
      try (AutoLogContext autoLogContext = autoLogContext(event)) {
        log.error("Exception occurred while handling {}", event.getClass().getSimpleName(), ex);
      }
      throw ex;
    } finally {
      try (AutoLogContext autoLogContext = autoLogContext(event)) {
        log.info(
            "[PMS_MESSAGE_LISTENER] Event Handler Processing Finished for {} event", event.getClass().getSimpleName());
      }
    }
  }

  protected abstract void handleEventWithContext(T event);

  protected AutoLogContext autoLogContext(T event) {
    Map<String, String> logContext = new HashMap<>();
    logContext.putAll(AmbianceUtils.logContextMap(extractAmbiance(event)));
    logContext.putAll(CollectionUtils.emptyIfNull(extraLogProperties(event)));
    return new AutoLogContext(logContext, OverrideBehavior.OVERRIDE_NESTS);
  }
}
