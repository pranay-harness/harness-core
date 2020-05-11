package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.adviser.impl.retry.RetryAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delay.DelayEventHelper;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.resume.EngineWaitRetryCallback;
import io.harness.execution.NodeExecution;
import io.harness.persistence.HPersistence;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

@OwnedBy(CDC)
@Slf4j
public class RetryHandler implements AdviseHandler<RetryAdvise> {
  @Inject AmbianceHelper ambianceHelper;
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private ExecutionEngine engine;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelayEventHelper delayEventHelper;

  @Override
  public void handleAdvise(Ambiance ambiance, RetryAdvise advise) {
    if (advise.getWaitInterval() > 0) {
      logger.info("Retry Wait Interval : {}", advise.getWaitInterval());
      String resumeId = delayEventHelper.delay(advise.getWaitInterval(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(ORCHESTRATION, new EngineWaitRetryCallback(), resumeId);
      return;
    }
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    String newUuid = generateUuid();
    NodeExecution newNodeExecution = nodeExecution.deepCopy();
    newNodeExecution.setStartTs(null);
    List<String> retryIds = newNodeExecution.getRetryIds() == null ? new ArrayList<>() : newNodeExecution.getRetryIds();
    retryIds.add(0, newUuid);
    newNodeExecution.setRetryIds(retryIds);
    newNodeExecution.setUuid(nodeExecution.getUuid());
    hPersistence.save(newNodeExecution);
    nodeExecution.setUuid(newUuid);
    hPersistence.save(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(ambiance).executionEngine(engine).build());
  }
}
