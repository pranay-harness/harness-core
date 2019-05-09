package io.harness.event.handler.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.event.model.EventType.DEPLOYMENT_VERIFIED;
import static software.wings.common.VerificationConstants.VERIFICATION_DEPLOYMENTS;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hazelcast.util.Preconditions;
import groovy.util.logging.Slf4j;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.metrics.HarnessMetricRegistry;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;

import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class VerificationEventHandler implements EventHandler {
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Inject
  public VerificationEventHandler(EventListener eventListener) {
    registerEventHandlers(eventListener);
  }

  private void registerEventHandlers(EventListener eventListener) {
    eventListener.registerEventHandler(this, Sets.newHashSet(DEPLOYMENT_VERIFIED));
  }

  @Override
  public void handleEvent(Event event) {
    Preconditions.checkTrue(event.getEventType() == DEPLOYMENT_VERIFIED, "Unknown event type " + event.getEventType());

    Map<String, String> properties = event.getEventData().getProperties();

    Preconditions.checkNotNull(properties.get("accountId"));
    Preconditions.checkNotNull(properties.get("workflowExecutionId"));
    Preconditions.checkNotNull(properties.get("rollback"));

    PageRequest<ContinuousVerificationExecutionMetaData> cvPageRequest =
        aPageRequest()
            .addFilter("accountId", SearchFilter.Operator.IN, properties.get("accountId"))
            .addFilter("workflowExecutionId", SearchFilter.Operator.EQ, properties.get("workflowExecutionId"))
            .addFieldsIncluded("accountId", "serviceId", "stateType", "executionStatus", "executionStatus", "noData")
            .build();
    List<ContinuousVerificationExecutionMetaData> cvExecutionMetaDataList =
        continuousVerificationService.getCVDeploymentData(cvPageRequest);

    if (!isEmpty(cvExecutionMetaDataList)) {
      boolean rolledback = Boolean.valueOf(properties.get("rollback"));

      for (ContinuousVerificationExecutionMetaData cvExecutionMetaData : cvExecutionMetaDataList) {
        harnessMetricRegistry.recordCounterInc(VERIFICATION_DEPLOYMENTS, cvExecutionMetaData.getAccountId(),
            cvExecutionMetaData.getServiceId(), cvExecutionMetaData.getStateType().name(),
            cvExecutionMetaData.getExecutionStatus().name(), String.valueOf(!cvExecutionMetaData.isNoData()),
            String.valueOf(false),
            String.valueOf(rolledback && cvExecutionMetaData.getExecutionStatus() == ExecutionStatus.FAILED));
      }
    }
  }
}
