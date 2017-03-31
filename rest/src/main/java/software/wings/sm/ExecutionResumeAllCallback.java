package software.wings.sm;

import com.google.inject.Inject;

import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Callback method for handling notify callback from wait notify engine.
 *
 * @author Rishi
 */
public class ExecutionResumeAllCallback implements NotifyCallback {
  @Inject private StateMachineExecutor stateMachineExecutor;

  private String appId;
  private String stateExecutionInstanceId;

  /**
   * Instantiates a new state machine resume callback.
   */
  public ExecutionResumeAllCallback() {}

  /**
   * Instantiates a new state machine resume callback.
   *
   * @param appId                    the app id
   * @param stateExecutionInstanceId the state execution instance id
   */
  public ExecutionResumeAllCallback(String appId, String stateExecutionInstanceId) {
    this.appId = appId;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets state execution instance id.
   *
   * @return the state execution instance id
   */
  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  /**
   * Sets state execution instance id.
   *
   * @param stateExecutionInstanceId the state execution instance id
   */
  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  /* (non-Javadoc)
   * @see software.wings.waitnotify.NotifyCallback#notify(java.util.Map)
   */
  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    stateMachineExecutor.startExecution(appId, stateExecutionInstanceId);
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    // Do nothing.
  }
}
