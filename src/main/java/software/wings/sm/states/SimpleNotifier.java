/**
 *
 */

package software.wings.sm.states;

import software.wings.waitnotify.WaitNotifyEngine;

import java.io.Serializable;

/**
 * @author Rishi
 */
public class SimpleNotifier implements Runnable {
  private WaitNotifyEngine waitNotifyEngine;
  private String correlationId;
  private Serializable response;

  public SimpleNotifier(WaitNotifyEngine waitNotifyEngine, String correlationId, Serializable response) {
    this.waitNotifyEngine = waitNotifyEngine;
    this.correlationId = correlationId;
    this.response = response;
  }

  @Override
  public void run() {
    waitNotifyEngine.notify(correlationId, response);
  }
}
