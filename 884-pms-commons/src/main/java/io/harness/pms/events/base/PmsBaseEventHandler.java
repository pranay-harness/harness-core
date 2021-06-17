package io.harness.pms.events.base;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.AutoLogContext.OverrideBehavior;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.monitoring.EventMonitoringService;
import io.harness.monitoring.MonitoringInfo;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.rbac.PrincipalTypeProtoToPrincipalTypeMapper;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ApiKeyPrincipal;
import io.harness.security.dto.ServicePrincipal;
import io.harness.security.dto.UserPrincipal;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;

public abstract class PmsBaseEventHandler<T extends Message> {
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

  public boolean handleEvent(T event, Map<String, String> metadataMap, long createdAt) {
    try (PmsGitSyncBranchContextGuard ignore1 = gitSyncContext(event); AutoLogContext ignore2 = autoLogContext(event)) {
      if (extractAmbiance(event) != null) {
        SecurityContextBuilder.setContext(getPrincipalFromAmbiance(extractAmbiance(event)));
      }
      ThreadAutoLogContext metricContext =
          new ThreadAutoLogContext(extractMetricContext(event), OverrideBehavior.OVERRIDE_NESTS);
      MonitoringInfo monitoringInfo = MonitoringInfo.builder()
                                          .createdAt(createdAt)
                                          .metricPrefix(getMetricPrefix(event))
                                          .metricContext(metricContext)
                                          .build();
      eventMonitoringService.sendMetric(LISTENER_START_METRIC, monitoringInfo, metadataMap);
      boolean isSuccess = handleEventWithContext(event);
      eventMonitoringService.sendMetric(LISTENER_END_METRIC, monitoringInfo, metadataMap);
      return isSuccess;
    }
  }

  protected abstract boolean handleEventWithContext(T event);

  protected AutoLogContext autoLogContext(T event) {
    Map<String, String> logContext = new HashMap<>();
    logContext.putAll(AmbianceUtils.logContextMap(extractAmbiance(event)));
    logContext.putAll(CollectionUtils.emptyIfNull(extraLogProperties(event)));
    return new AutoLogContext(logContext, OverrideBehavior.OVERRIDE_NESTS);
  }

  protected io.harness.security.dto.Principal getPrincipalFromAmbiance(Ambiance ambiance) {
    ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    ExecutionTriggerInfo executionTriggerInfo = ambiance.getMetadata().getTriggerInfo();

    // NOTE: rbac should not be validated for triggers so all the resources should be validated with service principals
    if (!executionPrincipalInfo.getShouldValidateRbac()) {
      return new ServicePrincipal(PIPELINE_SERVICE.getServiceId());
    }

    String principal = executionPrincipalInfo.getPrincipal();
    if (EmptyPredicate.isEmpty(principal)) {
      throw new AccessDeniedException("Execution with empty principal found. Please contact harness customer care.",
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
    PrincipalType principalType = PrincipalTypeProtoToPrincipalTypeMapper.convertToAccessControlPrincipalType(
        executionPrincipalInfo.getPrincipalType());
    switch (principalType) {
      case USER:
        return new UserPrincipal(principal, executionTriggerInfo.getTriggeredBy().getExtraInfoMap().get("email"),
            executionTriggerInfo.getTriggeredBy().getIdentifier(), AmbianceUtils.getAccountId(ambiance));
      case SERVICE:
        return new ServicePrincipal(principal);
      case API_KEY:
        return new ApiKeyPrincipal(principal);
    }
    throw new AccessDeniedException("Unknown Principal Type", WingsException.USER);
  }
}
