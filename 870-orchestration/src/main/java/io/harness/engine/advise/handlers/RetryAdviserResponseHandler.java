package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delay.DelayEventHelper;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.interrupts.helpers.RetryHelper;
import io.harness.engine.resume.EngineWaitRetryCallback;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.advisers.RetryAdvise;
import io.harness.pms.ambiance.Ambiance;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class RetryAdviserResponseHandler implements AdviserResponseHandler {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private RetryHelper retryHelper;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void handleAdvise(Ambiance ambiance, AdviserResponse adviserResponse) {
    RetryAdvise advise = adviserResponse.getRetryAdvise();
    if (advise.getWaitInterval() > 0) {
      log.info("Retry Wait Interval : {}", advise.getWaitInterval());
      String resumeId = delayEventHelper.delay(advise.getWaitInterval(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(publisherName,
          new EngineWaitRetryCallback(ambiance.getPlanExecutionId(), advise.getRetryNodeExecutionId()), resumeId);
      return;
    }
    retryHelper.retryNodeExecution(advise.getRetryNodeExecutionId(), null);
  }
}
