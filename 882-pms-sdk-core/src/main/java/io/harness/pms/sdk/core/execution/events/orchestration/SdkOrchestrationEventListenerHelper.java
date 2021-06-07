package io.harness.pms.sdk.core.execution.events.orchestration;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.PmsSdkModuleUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.pms.sdk.core.registries.OrchestrationEventHandlerRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class SdkOrchestrationEventListenerHelper {
  @Inject private OrchestrationEventHandlerRegistry handlerRegistry;
  @Inject @Named(PmsSdkModuleUtils.SDK_EXECUTOR_NAME) private ExecutorService executorService;

  public void handleEvent(OrchestrationEvent orchestrationEvent) {
    Set<OrchestrationEventHandler> handlers = handlerRegistry.obtain(orchestrationEvent.getEventType());
    if (isNotEmpty(handlers)) {
      handlers.forEach(handler -> executorService.submit(() -> handler.handleEvent(orchestrationEvent)));
    }
  }
}
