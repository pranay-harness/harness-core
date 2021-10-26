package io.harness.pms.sdk.execution.events.orchestrationevent;

import static io.harness.pms.sdk.PmsSdkModuleUtils.ORCHESTRATION_EVENT_EXECUTOR_NAME;
import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.orchestration.SdkOrchestrationEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventMessageListener
    extends PmsAbstractMessageListener<OrchestrationEvent, SdkOrchestrationEventHandler> {
  @Override
  public boolean isProcessable(Message message) {
    return true;
  }

  @Inject
  public OrchestrationEventMessageListener(@Named(SDK_SERVICE_NAME) String serviceName,
      SdkOrchestrationEventHandler sdkOrchestrationEventHandler,
      @Named(ORCHESTRATION_EVENT_EXECUTOR_NAME) ExecutorService executorService) {
    super(serviceName, OrchestrationEvent.class, sdkOrchestrationEventHandler, executorService);
  }
}
