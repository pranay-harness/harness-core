/**
 *
 */

package software.wings.sm.states;

import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

/**
 * @author Rishi
 */
public class SimpleNotifier implements Runnable {
  private WaitNotifyEngine waitNotifyEngine;
  private String correlationId;
  private NotifyResponseData response;

  public SimpleNotifier(WaitNotifyEngine waitNotifyEngine, String correlationId, NotifyResponseData response) {
    this.waitNotifyEngine = waitNotifyEngine;
    this.correlationId = correlationId;
    this.response = response;
  }

  @Override
  public void run() {
    waitNotifyEngine.notify(correlationId, response);
  }
}
