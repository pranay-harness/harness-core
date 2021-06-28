package io.harness.engine.interrupts.consumers;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.pms.contracts.interrupts.InterruptEventResponseProto;
import io.harness.pms.events.base.PmsAbstractMessageListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class InterruptResponseMessageListener
    extends PmsAbstractMessageListener<InterruptEventResponseProto, InterruptResponseHandler> {
  @Inject
  public InterruptResponseMessageListener(@Named(SDK_SERVICE_NAME) String serviceName, InterruptResponseHandler handler,
      @Named("EngineExecutorService") ExecutorService executorService) {
    super(serviceName, InterruptEventResponseProto.class, handler, executorService);
  }

  @Override
  public boolean isProcessable(Message message) {
    return true;
  }
}
