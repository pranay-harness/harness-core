package io.harness.pms.sdk.core.execution.events.node.advise;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviseEvent;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.AdviserRegistry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class NodeAdviseEventHandler extends PmsBaseEventHandler<AdviseEvent> {
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;

  @Override
  protected Map<String, String> extractMetricContext(AdviseEvent message) {
    return ImmutableMap.<String, String>builder()
        .put("accountId", AmbianceUtils.getAccountId(message.getAmbiance()))
        .put("projectIdentifier", AmbianceUtils.getOrgIdentifier(message.getAmbiance()))
        .put("orgIdentifier", AmbianceUtils.getProjectIdentifier(message.getAmbiance()))
        .build();
  }

  @Override
  protected String getMetricPrefix(AdviseEvent message) {
    return "advise_event";
  }

  @NotNull
  @Override
  protected Map<String, String> extraLogProperties(AdviseEvent event) {
    return ImmutableMap.<String, String>builder()
        .put("eventType", NodeExecutionEventType.ADVISE.name())
        .put("notifyId", event.getNotifyId())
        .build();
  }

  @Override
  protected Ambiance extractAmbiance(AdviseEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected void handleEventWithContext(AdviseEvent event) {
    try {
      log.info("Starting to handle ADVISE event");

      String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(event.getAmbiance());
      Preconditions.checkArgument(isNotBlank(nodeExecutionId), "nodeExecutionId is null or empty");

      AdviserResponse adviserResponse = null;
      for (AdviserObtainment obtainment : event.getAdviserObtainmentsList()) {
        Adviser adviser = adviserRegistry.obtain(obtainment.getType());
        AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                          .ambiance(event.getAmbiance())
                                          .failureInfo(event.getFailureInfo())
                                          .isPreviousAdviserExpired(event.getIsPreviousAdviserExpired())
                                          .retryIds(event.getRetryIdsList())
                                          .toStatus(event.getToStatus())
                                          .fromStatus(event.getFromStatus())
                                          .adviserParameters(obtainment.getParameters().toByteArray())
                                          .build();
        if (adviser.canAdvise(advisingEvent)) {
          adviserResponse = adviser.onAdviseEvent(advisingEvent);
          if (adviserResponse != null) {
            break;
          }
        }
      }

      if (adviserResponse != null) {
        sdkNodeExecutionService.handleAdviserResponse(nodeExecutionId, event.getNotifyId(), adviserResponse);
      } else {
        log.info("Calculated Adviser response is null. Proceeding with UNKNOWN adviser type.");
        sdkNodeExecutionService.handleAdviserResponse(
            nodeExecutionId, event.getNotifyId(), AdviserResponse.newBuilder().setType(AdviseType.UNKNOWN).build());
      }

      log.info("ADVISE Event Handled Successfully");
    } catch (Exception ex) {
      log.error("Error while advising execution", ex);
      sdkNodeExecutionService.handleEventError(
          NodeExecutionEventType.ADVISE, event.getNotifyId(), NodeExecutionUtils.constructFailureInfo(ex));
    }
  }
}
