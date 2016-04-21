package software.wings.sm;

import software.wings.app.WingsBootstrap;
import software.wings.waitnotify.NotifyCallback;

import java.io.Serializable;
import java.util.Map;

/**
 * Callback method for handling notify callback from wait notify engine.
 * @author Rishi
 */
public class SmAsynchResumeCallback implements NotifyCallback {
  private static final long serialVersionUID = 1L;

  private String smInstanceId;

  public SmAsynchResumeCallback(String smInstanceId) {
    this.smInstanceId = smInstanceId;
  }

  /* (non-Javadoc)
   * @see software.wings.waitnotify.NotifyCallback#notify(java.util.Map)
   */
  @Override
  public void notify(Map<String, ? extends Serializable> response) {
    WingsBootstrap.lookup(StateMachineExecutor.class).resume(smInstanceId, response);
  }
}
