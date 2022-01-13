/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.deploymentfreezewindow;

import static io.harness.beans.FeatureName.NEW_DEPLOYMENT_FREEZE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.governance.TimeRangeBasedFreezeConfig;

import software.wings.beans.governance.GovernanceConfig;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.deploymentfreezewindow.input.QLToggleDeploymentFreezeWindowInput;
import software.wings.graphql.schema.mutation.deploymentfreezewindow.payload.QLToggleDeploymentFreezeWindowPayload;
import software.wings.graphql.schema.type.deploymentfreezewindow.QLDeploymentFreezeWindow;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class ToggleDeploymentFreezeWindowDataFetcher
    extends BaseMutatorDataFetcher<QLToggleDeploymentFreezeWindowInput, QLToggleDeploymentFreezeWindowPayload> {
  @Inject FeatureFlagService featureFlagService;
  @Inject GovernanceConfigService governanceConfigService;
  @Inject DeploymentFreezeWindowController deploymentFreezeWindowController;

  public ToggleDeploymentFreezeWindowDataFetcher() {
    super(QLToggleDeploymentFreezeWindowInput.class, QLToggleDeploymentFreezeWindowPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_DEPLOYMENT_FREEZES)
  protected QLToggleDeploymentFreezeWindowPayload mutateAndFetch(
      QLToggleDeploymentFreezeWindowInput parameter, MutationContext mutationContext) {
    String accountId = mutationContext.getAccountId();

    // FEATURE FLAG CHECK
    if (!featureFlagService.isEnabled(NEW_DEPLOYMENT_FREEZE, mutationContext.getAccountId())) {
      throw new InvalidRequestException("Please enable feature flag to toggle the deployment freeze window");
    }

    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigList = governanceConfig.getTimeRangeBasedFreezeConfigs();
    TimeRangeBasedFreezeConfig existingDeploymentFreezeWindow = null;

    // GETTING THE DEPLOYMENT FREEZE WINDOW FOR THE GIVEN ID
    for (TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig : timeRangeBasedFreezeConfigList) {
      if (timeRangeBasedFreezeConfig.getUuid().equals(parameter.getId())) {
        existingDeploymentFreezeWindow = timeRangeBasedFreezeConfig;
        break;
      }
    }

    // CHECK IF DFW DOESN'T EXIST
    if (existingDeploymentFreezeWindow == null) {
      throw new InvalidRequestException("No Freeze Window exists with the given ID");
    }

    // TOGGLING THE WINDOW
    if (existingDeploymentFreezeWindow.isApplicable() != parameter.getApplicable()) {
      existingDeploymentFreezeWindow.setApplicable(parameter.getApplicable());
    } else {
      if (parameter.getApplicable()) {
        throw new InvalidRequestException("The deployment freeze window is already enabled.");
      } else {
        throw new InvalidRequestException("The deployment freeze window is already disabled.");
      }
    }

    int listSize = timeRangeBasedFreezeConfigList.size();
    for (int i = 0; i < listSize; i++) {
      if (timeRangeBasedFreezeConfigList.get(i).getUuid().equals(existingDeploymentFreezeWindow.getUuid())) {
        timeRangeBasedFreezeConfigList.set(i, existingDeploymentFreezeWindow);
        break;
      }
    }

    // SAVING THE UPDATED DEPLOYMENT FREEZE WINDOW
    governanceConfig.setTimeRangeBasedFreezeConfigs(timeRangeBasedFreezeConfigList);
    governanceConfigService.upsert(accountId, governanceConfig);

    // PAYLOAD FOR THE UPDATED DEPLOYMENT FREEZE WINDOW
    QLDeploymentFreezeWindow qlDeploymentFreezeWindow =
        deploymentFreezeWindowController.populateDeploymentFreezeWindowPayload(existingDeploymentFreezeWindow);

    return QLToggleDeploymentFreezeWindowPayload.builder()
        .clientMutationId(parameter.getClientMutationId())
        .deploymentFreezeWindow(qlDeploymentFreezeWindow)
        .build();
  }
}
