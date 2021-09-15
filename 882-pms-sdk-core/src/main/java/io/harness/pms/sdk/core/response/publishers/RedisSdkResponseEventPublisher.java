/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.response.publishers;

import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.SDK_RESPONSE_EVENT_PRODUCER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.manage.GlobalContextManager;
import io.harness.monitoring.MonitoringContext;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisSdkResponseEventPublisher implements SdkResponseEventPublisher {
  private Producer eventProducer;

  @Inject
  public RedisSdkResponseEventPublisher(@Named(SDK_RESPONSE_EVENT_PRODUCER) Producer producer) {
    this.eventProducer = producer;
  }

  @Override
  public void publishEvent(SdkResponseEventProto event) {
    MonitoringContext monitoringContext = GlobalContextManager.get(MonitoringContext.IS_MONITORING_ENABLED);
    Map<String, String> metadataMap = new HashMap<>();
    if (monitoringContext != null) {
      metadataMap.put(PIPELINE_MONITORING_ENABLED, String.valueOf(monitoringContext.isMonitoringEnabled()));
    } else {
      metadataMap.put(PIPELINE_MONITORING_ENABLED, "false");
    }

    metadataMap.put("eventType", event.getSdkResponseEventType().name());
    metadataMap.put("nodeExecutionId", event.getNodeExecutionId());
    metadataMap.put("planExecutionId", event.getPlanExecutionId());
    eventProducer.send(Message.newBuilder().putAllMetadata(metadataMap).setData(event.toByteString()).build());
  }
}
