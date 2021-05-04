package io.harness.service.stats.usagemetrics.eventconsumer;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.eventsframework.NgEventLogContext;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.timeseriesevent.TimeseriesBatchEventInfo;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.event.MessageListener;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceStatsEventListener implements MessageListener {
  @Override
  public boolean handleMessage(Message message) {
    final String messageId = message.getId();
    log.info("Processing the instance stats timescale event with the id {}", messageId);
    try (AutoLogContext ignore1 = new NgEventLogContext(messageId, OVERRIDE_ERROR)) {
      TimeseriesBatchEventInfo eventInfo = TimeseriesBatchEventInfo.parseFrom(message.getMessage().getData());
      //

      return true;
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking TimeseriesBatchEventInfo for key {}", message.getId(), e);
      return false;
    } catch (Exception ex) {
      log.error("Unchecked exception faced during handling TimeseriesBatchEventInfo for key {}", message.getId(), ex);
      return false;
    }
  }
}
