package io.harness.execution.consumers;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.execution.utils.SdkResponseHandler;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.events.base.PmsAbstractMessageListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class SdkResponseEventMessageListener extends PmsAbstractMessageListener<SdkResponseEventProto> {
  private final SdkResponseHandler redisSdkResponseHandler;

  @Inject
  public SdkResponseEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, SdkResponseHandler redisSdkResponseHandler) {
    super(serviceName, SdkResponseEventProto.class);
    this.redisSdkResponseHandler = redisSdkResponseHandler;
  }

  @Override
  public boolean processMessage(
      SdkResponseEventProto sdkResponseEventProto, Map<String, String> metadataMap, Long timestamp) {
    redisSdkResponseHandler.handleEvent(sdkResponseEventProto, metadataMap, timestamp);
    return true;
  }

  @Override
  public boolean isProcessable(Message message) {
    return true;
  }
}
