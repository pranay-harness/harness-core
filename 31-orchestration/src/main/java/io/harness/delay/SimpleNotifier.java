package io.harness.delay;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class SimpleNotifier implements Runnable {
  private WaitNotifyEngine waitNotifyEngine;
  private String correlationId;
  private ResponseData response;

  /**
   * Instantiates a new Simple notifier.
   *
   * @param waitNotifyEngine the wait notify engine
   * @param correlationId    the correlation id
   * @param response         the response
   */
  public SimpleNotifier(WaitNotifyEngine waitNotifyEngine, String correlationId, ResponseData response) {
    this.waitNotifyEngine = waitNotifyEngine;
    this.correlationId = correlationId;
    this.response = response;
  }

  @Override
  public void run() {
    logger.info("Simple Notifier Notifying on correlation id : {}", correlationId);
    waitNotifyEngine.doneWith(correlationId, response);
  }
}
