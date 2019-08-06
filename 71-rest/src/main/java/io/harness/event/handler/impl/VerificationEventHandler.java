package io.harness.event.handler.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.event.model.EventConstants.ACCOUNT_ID;
import static io.harness.event.model.EventConstants.IS_24X7_ENABLED;
import static io.harness.event.model.EventConstants.VERIFICATION_STATE_TYPE;
import static io.harness.event.model.EventType.CV_META_DATA;
import static io.harness.event.model.EventType.DEPLOYMENT_VERIFIED;
import static software.wings.common.VerificationConstants.VERIFICATION_DEPLOYMENTS;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hazelcast.util.Preconditions;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.metrics.HarnessMetricRegistry;
import lombok.extern.slf4j.Slf4j;
import software.wings.common.VerificationConstants;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationService;

import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class VerificationEventHandler implements EventHandler {
  @Inject private HarnessMetricRegistry harnessMetricRegistry;
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Inject
  public VerificationEventHandler(EventListener eventListener) {
    registerEventHandlers(eventListener);
  }

  private void registerEventHandlers(EventListener eventListener) {
    eventListener.registerEventHandler(this, Sets.newHashSet(DEPLOYMENT_VERIFIED, CV_META_DATA));
  }

  @Override
  public void handleEvent(Event event) {
    switch (event.getEventType()) {
      case DEPLOYMENT_VERIFIED:
        handleDeploymentEvent(event);
        break;
      case CV_META_DATA:
        handleCVMetaDataEvent(event);
        break;
      default:
        logger.error("Invalid event type, dropping event : [{}]", event);
    }
  }

  private void handleDeploymentEvent(Event event) {
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

  private void handleCVMetaDataEvent(Event event) {
    Map<String, String> properties = event.getEventData().getProperties();

    Preconditions.checkNotNull(properties.get(ACCOUNT_ID));
    Preconditions.checkNotNull(properties.get(VERIFICATION_STATE_TYPE));
    Preconditions.checkNotNull(properties.get(IS_24X7_ENABLED));

    harnessMetricRegistry.recordGaugeValue(VerificationConstants.CV_META_DATA,
        new String[] {properties.get(ACCOUNT_ID), properties.get(VERIFICATION_STATE_TYPE),
            String.valueOf(properties.get(IS_24X7_ENABLED))},
        event.getEventData().getValue());
  }
}
