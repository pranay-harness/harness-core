/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import software.wings.api.EmailStateExecutionData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

import com.github.reinert.jjschema.Attributes;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
@Attributes
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class PauseState extends EmailState {
  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public PauseState(String name) {
    super(name);
    setStateType(StateType.PAUSE.getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionResponse emailExecutionResponse = super.execute(context);

    EmailStateExecutionData emailStateExecutionData =
        (EmailStateExecutionData) emailExecutionResponse.getStateExecutionData();

    return ExecutionResponse.builder()
        .stateExecutionData(emailStateExecutionData)
        .executionStatus(ExecutionStatus.PAUSED)
        .build();
  }

  @DefaultValue(
      value =
          "${workflow.name} execution paused at manual step: ${displayName}. Please click on the link below to resume :\n ${workflow.url}")
  @Attributes(title = "Body")
  @Override
  public String
  getBody() {
    return super.getBody();
  }

  @Attributes(title = "CC")
  @Override
  public String getCcAddress() {
    return super.getCcAddress();
  }

  @Attributes(title = "To", required = true)
  @Override
  public String getToAddress() {
    return super.getToAddress();
  }

  @DefaultValue(value = "Action Required - ${workflow.name} execution paused at manual step ${currentState}")
  @Attributes(title = "Subject", required = true)
  @Override
  public String getSubject() {
    return super.getSubject();
  }

  @Attributes(title = "Ignore Delivery Failure?")
  @Override
  public Boolean isIgnoreDeliveryFailure() {
    return super.isIgnoreDeliveryFailure();
  }
}
