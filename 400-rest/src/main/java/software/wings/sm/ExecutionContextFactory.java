/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * Created by rishi on 8/2/16.
 */
@OwnedBy(CDC)
@Singleton
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ExecutionContextFactory {
  @Inject private Injector injector;

  /**
   * Create execution context execution context.
   *
   * @param stateExecutionInstance the state execution instance
   * @param stateMachine           the state machine
   * @return the execution context
   */
  public ExecutionContext createExecutionContext(
      StateExecutionInstance stateExecutionInstance, StateMachine stateMachine) {
    return new ExecutionContextImpl(stateExecutionInstance, stateMachine, injector);
  }
}
