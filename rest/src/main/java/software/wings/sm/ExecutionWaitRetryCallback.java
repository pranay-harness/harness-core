package software.wings.sm;

import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.RETRY;

import com.google.inject.Inject;

import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Callback method for handling notify callback from wait notify engine.
 *
 * @author Rishi
 */
public class ExecutionWaitRetryCallback implements NotifyCallback {
  @Inject private ExecutionInterruptManager executionInterruptManager;

  private String appId;
  private String executionUuid;
  private String stateExecutionInstanceId;

  /**
   * Instantiates a new state machine resume callback.
   */
  public ExecutionWaitRetryCallback() {}

  /**
   * Instantiates a new state machine resume callback.
   *
   * @param appId                    the app id
   * @param stateExecutionInstanceId the state execution instance id
   */
  public ExecutionWaitRetryCallback(String appId, String executionUuid, String stateExecutionInstanceId) {
    this.appId = appId;
    this.executionUuid = executionUuid;
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    executionInterruptManager.registerExecutionInterrupt(anExecutionInterrupt()
                                                             .withAppId(appId)
                                                             .withExecutionUuid(executionUuid)
                                                             .withStateExecutionInstanceId(stateExecutionInstanceId)
                                                             .withExecutionInterruptType(RETRY)
                                                             .build());
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    // Do nothing.
  }
}
