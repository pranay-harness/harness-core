package software.wings.sm;

import com.google.inject.Inject;

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

  @Inject private StateMachineExecutor stateMachineExecutor;

  private String appId;
  private String stateExecutionInstanceId;

  public StateMachineResumeCallback() {}

  public StateMachineResumeCallback(String appId, String stateExecutionInstanceId) {
    this.appId = appId;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  @Override
  public void notify(Map<String, ? extends Serializable> response) {
    stateMachineExecutor.resume(appId, stateExecutionInstanceId, response);
  }
}
