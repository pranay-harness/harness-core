/**
 *
 */
package software.wings.sm;

import java.io.Serializable;
import java.util.Map;

import software.wings.app.WingsBootstrap;
import software.wings.waitNotify.NotifyCallback;

/**
 * @author Rishi
 *
 */
public class SMAsynchResumeCallback implements NotifyCallback {
  private static final long serialVersionUID = 1L;

  private String smInstanceId;

  public SMAsynchResumeCallback(String smInstanceId) {
    this.smInstanceId = smInstanceId;
  }

  /* (non-Javadoc)
   * @see software.wings.waitNotify.NotifyCallback#notify(java.util.Map)
   */
  @Override
  public void notify(Map<String, ? extends Serializable> response) {
    WingsBootstrap.lookup(StateMachineExecutor.class).resume(smInstanceId, response);
  }
}
