package io.harness.pms.event.pollingevent;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;
import io.harness.ngtriggers.buildtriggers.eventmapper.BuildTriggerEventMapper;
import io.harness.polling.contracts.PollingResponse;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class PollingEventStreamListener implements MessageListener {
  private BuildTriggerEventMapper mapper;

  @Inject
  public PollingEventStreamListener(BuildTriggerEventMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      try {
        PollingResponse response = PollingResponse.parseFrom(message.getMessage().getData());
        mapper.consumeBuildTriggerEvent(response);
      } catch (InvalidProtocolBufferException e) {
        throw new InvalidRequestException("Exception in unpacking/processing of WebhookDTO event", e);
      }
    }
    return true;
  }
}
