/**
 *
 */

package software.wings.sm.states;

import io.harness.delegate.task.protocol.ResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

/**
 * The type Simple notifier.
 *
 * @author Rishi
 */
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
    waitNotifyEngine.notify(correlationId, response);
  }
}
