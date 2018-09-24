package software.wings.sm;

import com.google.inject.Inject;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Callback method for handling notify callback from wait notify engine.
 *
 * @author Rishi
 */
@Data
@NoArgsConstructor
public class ExecutionWaitCallback implements NotifyCallback {
  @Inject private StateMachineExecutor stateMachineExecutor;

  private String appId;
  private String executionUuid;
  private String stateExecutionInstanceId;

  /**
   * Instantiates a new state machine resume callback.
   *
   * @param appId                    the app id
   * @param stateExecutionInstanceId the state execution instance id
   */
  public ExecutionWaitCallback(String appId, String executionUuid, String stateExecutionInstanceId) {
    this.appId = appId;
    this.executionUuid = executionUuid;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    stateMachineExecutor.startStateExecution(appId, executionUuid, stateExecutionInstanceId);
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    // Do nothing.
  }
}
