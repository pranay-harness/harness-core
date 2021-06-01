package io.harness.execution.consumers;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.execution.utils.SdkResponseListenerHelper;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.events.base.PmsAbstractBaseMessageListenerWithObservers;
import io.harness.pms.execution.utils.SdkResponseEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class SdkResponseEventMessageListener
    extends PmsAbstractBaseMessageListenerWithObservers<SdkResponseEventProto> {
  private final SdkResponseListenerHelper sdkResponseListenerHelper;

  @Inject
  public SdkResponseEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, SdkResponseListenerHelper sdkResponseListenerHelper) {
    super(serviceName, SdkResponseEventProto.class);
    this.sdkResponseListenerHelper = sdkResponseListenerHelper;
  }

  @Override
  public boolean processMessageInternal(SdkResponseEventProto sdkResponseEventProto) {
    try {
      sdkResponseListenerHelper.handleEvent(SdkResponseEventUtils.fromProtoToSdkResponseEvent(sdkResponseEventProto));
      return true;
    } catch (Exception ex) {
      // TODO (prashant) : Handle Failure should we retry here. Currently acknowledging the message ?
      log.error("Processing failed for SdkResponseEvent", ex);
      return true;
    }
  }

  @Override
  public boolean isProcessable(Message message) {
    return true;
  }
}
