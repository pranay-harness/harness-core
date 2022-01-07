/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionInterruptType.RETRY;

import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.tasks.ResponseData;
import io.harness.waiter.OldNotifyCallback;

import com.google.inject.Inject;
import java.util.Map;

/**
 * Callback method for handling notify callback from wait notify engine.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ExecutionWaitRetryCallback implements OldNotifyCallback {
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
  public void notify(Map<String, ResponseData> response) {
    executionInterruptManager.registerExecutionInterrupt(anExecutionInterrupt()
                                                             .appId(appId)
                                                             .executionUuid(executionUuid)
                                                             .stateExecutionInstanceId(stateExecutionInstanceId)
                                                             .executionInterruptType(RETRY)
                                                             .build());
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    // Do nothing.
  }
}
