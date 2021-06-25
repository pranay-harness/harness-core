package io.harness.pms.sdk.core.response.publishers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.sdk.core.events.PipelineSdkEventSender;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisSdkResponseEventPublisher implements SdkResponseEventPublisher {
  @Inject private PipelineSdkEventSender pipelineSdkEventSender;

  @Override
  public void publishEvent(SdkResponseEventProto event) {
    pipelineSdkEventSender.sendEvent(event.toByteString(), PmsEventCategory.SDK_RESPONSE_EVENT, true);
  }
}
