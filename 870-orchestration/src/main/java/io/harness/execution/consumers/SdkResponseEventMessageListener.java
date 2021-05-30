package io.harness.execution.consumers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.utils.SdkResponseListenerHelper;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.pms.execution.utils.SdkResponseEventUtils;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseEventMessageListener implements MessageListener {
  @Inject private SdkResponseListenerHelper sdkResponseListenerHelper;

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      SdkResponseEvent sdkResponseEvent;
      try {
        SdkResponseEventProto sdkResponseEventProto = SdkResponseEventProto.parseFrom(message.getMessage().getData());
        sdkResponseEvent = SdkResponseEventUtils.fromProtoToSdkResponseEvent(sdkResponseEventProto);
      } catch (InvalidProtocolBufferException e) {
        throw new InvalidRequestException(
            String.format("Exception in unpacking SdkResponseEvent Proto for key %s", message.getId()), e);
      }
      processMessage(sdkResponseEvent);
    }
    return true;
  }

  private void processMessage(SdkResponseEvent sdkResponseEvent) {
    sdkResponseListenerHelper.handleEvent(sdkResponseEvent);
  }
}
