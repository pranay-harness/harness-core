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
import software.wings.graphql.schema.mutation.deploymentfreezewindow.input.QLCreateDeploymentFreezeWindowInput;
import software.wings.graphql.schema.mutation.deploymentfreezewindow.payload.QLDeploymentFreezeWindowPayload;
import software.wings.graphql.schema.type.deploymentfreezewindow.QLDeploymentFreezeWindow;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class CreateDeploymentFreezeWindowDataFetcher
    extends BaseMutatorDataFetcher<QLCreateDeploymentFreezeWindowInput, QLDeploymentFreezeWindowPayload> {
  @Inject FeatureFlagService featureFlagService;
  @Inject GovernanceConfigService governanceConfigService;
  @Inject DeploymentFreezeWindowController deploymentFreezeWindowController;

  @Inject
  public CreateDeploymentFreezeWindowDataFetcher() {
    super(QLCreateDeploymentFreezeWindowInput.class, QLDeploymentFreezeWindowPayload.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_DEPLOYMENT_FREEZES)
  protected QLDeploymentFreezeWindowPayload mutateAndFetch(
      QLCreateDeploymentFreezeWindowInput parameter, MutationContext mutationContext) {
    String accountId = mutationContext.getAccountId();

    // FEATURE FLAG CHECK
    if (!featureFlagService.isEnabled(NEW_DEPLOYMENT_FREEZE, mutationContext.getAccountId())) {
      throw new InvalidRequestException("Please enable feature flag to create deployment freeze window");
    }
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);

    // POPULATING DATA FROM INPUT
    TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig =
        deploymentFreezeWindowController.populateDeploymentFreezeWindowEntity(parameter);

    // VALIDATING INPUTS
    deploymentFreezeWindowController.validateDeploymentFreezeWindowInput(timeRangeBasedFreezeConfig, accountId);

    List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigList = governanceConfig.getTimeRangeBasedFreezeConfigs();

    if (timeRangeBasedFreezeConfigList == null) {
      timeRangeBasedFreezeConfigList = new ArrayList<>();
    }

    // SAVING THE DEPLOYMENT FREEZE WINDOW
    timeRangeBasedFreezeConfigList.add(timeRangeBasedFreezeConfig);
    governanceConfig.setTimeRangeBasedFreezeConfigs(timeRangeBasedFreezeConfigList);
    governanceConfigService.upsert(accountId, governanceConfig);

    log.info("Creating deployment freeze window with the name {} in account {} from graphql", parameter.getName(),
        accountId);

    // CREATING PAYLOAD
    QLDeploymentFreezeWindow qlDeploymentFreezeWindow =
        deploymentFreezeWindowController.populateDeploymentFreezeWindowPayload(timeRangeBasedFreezeConfig);

    return QLDeploymentFreezeWindowPayload.builder()
        .clientMutationId(timeRangeBasedFreezeConfig.getUuid())
        .deploymentFreezeWindow(qlDeploymentFreezeWindow)
        .build();
  }
}
