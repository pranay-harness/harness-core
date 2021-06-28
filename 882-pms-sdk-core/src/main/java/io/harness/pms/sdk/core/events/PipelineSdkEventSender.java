package io.harness.pms.sdk.core.events;

import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.manage.GlobalContextManager;
import io.harness.monitoring.MonitoringContext;
import io.harness.pms.events.base.PmsEventCategory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class PipelineSdkEventSender {
  public String sendEvent(
      Producer producer, ByteString eventData, PmsEventCategory eventCategory, boolean isMonitored) {
    log.info("Sending {} event for {} to the producer", eventCategory, ModuleType.PMS.name().toLowerCase());

    ImmutableMap.Builder<String, String> metadataBuilder =
        ImmutableMap.<String, String>builder().put(SERVICE_NAME, ModuleType.PMS.name().toLowerCase());
    MonitoringContext monitoringContext = GlobalContextManager.get(MonitoringContext.IS_MONITORING_ENABLED);
    if (isMonitored && monitoringContext != null) {
      metadataBuilder.put(PIPELINE_MONITORING_ENABLED, String.valueOf(monitoringContext.isMonitoringEnabled()));
    }

    String messageId =
        producer.send(Message.newBuilder().putAllMetadata(metadataBuilder.build()).setData(eventData).build());
    log.info("Successfully Sent {} event for {} to the producer. MessageId {}", eventCategory,
        ModuleType.PMS.name().toLowerCase(), messageId);
    return messageId;
  }
}
