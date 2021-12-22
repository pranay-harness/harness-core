package io.harness.engine.interrupts;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.OrchestrationEndObserver;
import io.harness.logging.AutoLogContext;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEndInterruptHandler implements AsyncInformObserver, OrchestrationEndObserver {
  @Inject private InterruptService interruptService;
  @Inject @Named("EngineHelperExecutorService") ExecutorService executorService;

  @Override
  public void onEnd(Ambiance ambiance) {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      long closedInterrupts = interruptService.closeActiveInterrupts(ambiance.getPlanExecutionId());
      if (closedInterrupts < 0) {
        log.error("Error Closing out the interrupts");
        return;
      }
      log.info("Closed {} active interrupts", closedInterrupts);
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
