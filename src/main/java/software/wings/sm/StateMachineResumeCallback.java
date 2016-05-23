package software.wings.sm;

import software.wings.app.WingsBootstrap;
import software.wings.waitnotify.NotifyCallback;

import java.io.Serializable;
import java.util.Map;

/**
 * Callback method for handling notify callback from wait notify engine.
 *
 * @author Rishi
 */
public class StateMachineResumeCallback implements NotifyCallback {
  private static final long serialVersionUID = 1L;

  private String appId;
  private String stateExecutionInstanceId;

  public StateMachineResumeCallback(String appId, String stateExecutionInstanceId) {
    this.appId = appId;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  @Override
  public void notify(Map<String, ? extends Serializable> response) {
    WingsBootstrap.lookup(StateMachineExecutor.class).resume(appId, stateExecutionInstanceId, response);
  }
}
