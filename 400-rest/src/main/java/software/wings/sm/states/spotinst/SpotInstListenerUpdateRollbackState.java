/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.spotinst.model.SpotInstConstants.DEPLOYMENT_ERROR;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.RENAME_NEW_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.RENAME_OLD_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.SWAP_ROUTES_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;

import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.SpotinstDummyCommandUnit;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDP)
public class SpotInstListenerUpdateRollbackState extends SpotInstListenerUpdateState {
  @Inject private transient SpotInstStateHelper spotInstStateHelper;

  @Override
  protected SpotInstSwapRoutesTaskParameters getTaskParameters(ExecutionContext context, Application app,
      String activityId, AwsAmiInfrastructureMapping awsAmiInfrastructureMapping,
      SpotInstSetupContextElement setupContextElement) {
    SpotInstSwapRoutesTaskParameters taskParameters =
        super.getTaskParameters(context, app, activityId, awsAmiInfrastructureMapping, setupContextElement);
    taskParameters.setNewElastiGroup(spotInstStateHelper.prepareNewElastiGroupConfigForRollback(setupContextElement));
    taskParameters.setOldElastiGroup(spotInstStateHelper.prepareOldElastiGroupConfigForRollback(setupContextElement));
    return taskParameters;
  }

  public SpotInstListenerUpdateRollbackState(String name) {
    super(name, StateType.SPOTINST_LISTENER_UPDATE_ROLLBACK.name());
  }

  @Override
  @SchemaIgnore
  public boolean isRollback() {
    return true;
  }

  @Override
  @SchemaIgnore
  public boolean isDownsizeOldElastiGroup() {
    return super.isDownsizeOldElastiGroup();
  }

  @Override
  protected ImmutableList<CommandUnit> getCommandUnits() {
    return ImmutableList.of(new SpotinstDummyCommandUnit(UP_SCALE_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(RENAME_OLD_COMMAND_UNIT), new SpotinstDummyCommandUnit(SWAP_ROUTES_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DOWN_SCALE_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT),
        new SpotinstDummyCommandUnit(RENAME_NEW_COMMAND_UNIT), new SpotinstDummyCommandUnit(DEPLOYMENT_ERROR));
  }
}
