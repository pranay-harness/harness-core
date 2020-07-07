package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationPublisherName;
import io.harness.adviser.advise.RetryAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delay.DelayEventHelper;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.interrupts.helpers.RetryHelper;
import io.harness.engine.resume.EngineWaitRetryCallback;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@OwnedBy(CDC)
@Slf4j
public class RetryAdviseHandler implements AdviseHandler<RetryAdvise> {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private RetryHelper retryHelper;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void handleAdvise(Ambiance ambiance, RetryAdvise advise) {
    if (advise.getWaitInterval() > 0) {
      logger.info("Retry Wait Interval : {}", advise.getWaitInterval());
      String resumeId = delayEventHelper.delay(advise.getWaitInterval(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(publisherName,
          new EngineWaitRetryCallback(ambiance.getPlanExecutionId(), advise.getRetryNodeExecutionId()), resumeId);
      return;
    }
    retryHelper.retryNodeExecution(advise.getRetryNodeExecutionId());
  }
}
