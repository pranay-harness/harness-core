package io.harness.engine.facilitation.facilitator.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.beans.FeatureName;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.execution.NodeExecution;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class RedisFacilitateEventPublisher implements FacilitateEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;
  @Inject private PmsFeatureFlagService pmsFeatureFlagService;

  @Override
  public String publishEvent(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    FacilitatorEvent event = FacilitatorEvent.newBuilder()
                                 .setNodeExecutionId(nodeExecutionId)
                                 .setAmbiance(nodeExecution.getAmbiance())
                                 .setStepParameters(nodeExecution.getNode().getStepParametersBytes())
                                 .setStepType(nodeExecution.getNode().getStepType())
                                 .setNotifyId(generateUuid())
                                 .addAllRefObjects(nodeExecution.getNode().getRebObjectsList())
                                 .addAllFacilitatorObtainments(nodeExecution.getNode().getFacilitatorObtainmentsList())
                                 .build();

    Producer producer =
        eventsFrameworkUtils.obtainProducerForFacilitationEvent(nodeExecution.getNode().getServiceName());
    Map<String, String> metadataMap = new HashMap<>();
    metadataMap.put(SERVICE_NAME, nodeExecution.getNode().getServiceName());
    if (pmsFeatureFlagService.isEnabled(
            AmbianceUtils.getAccountId(event.getAmbiance()), FeatureName.PIPELINE_MONITORING)) {
      metadataMap.put(PIPELINE_MONITORING_ENABLED, "true");
    }
    producer.send(Message.newBuilder().putAllMetadata(metadataMap).setData(event.toByteString()).build());

    return event.getNotifyId();
  }
}
