package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.waiter.NotifyCallback;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Callback method for handling notify callback from wait notify engine.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@Data
@NoArgsConstructor
public class ExecutionResumeAllCallback implements NotifyCallback {
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
  public ExecutionResumeAllCallback(String appId, String executionUuid, String stateExecutionInstanceId) {
    this.appId = appId;
    this.executionUuid = executionUuid;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    stateMachineExecutor.startExecution(appId, executionUuid, stateExecutionInstanceId);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // Do nothing.
  }
}
