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
import software.wings.graphql.schema.mutation.deploymentfreezewindow.input.QLDeleteDeploymentFreezeWindowInput;
import software.wings.graphql.schema.mutation.deploymentfreezewindow.payload.QLDeleteDeploymentFreezeWindowPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class DeleteDeploymentFreezeWindowDataFetcher
    extends BaseMutatorDataFetcher<QLDeleteDeploymentFreezeWindowInput, QLDeleteDeploymentFreezeWindowPayload> {
  @Inject FeatureFlagService featureFlagService;
  @Inject GovernanceConfigService governanceConfigService;

  @Inject
  public DeleteDeploymentFreezeWindowDataFetcher(GovernanceConfigService governanceConfigService) {
    super(QLDeleteDeploymentFreezeWindowInput.class, QLDeleteDeploymentFreezeWindowPayload.class);

    this.governanceConfigService = governanceConfigService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_DEPLOYMENT_FREEZES)
  public QLDeleteDeploymentFreezeWindowPayload mutateAndFetch(
      QLDeleteDeploymentFreezeWindowInput parameter, MutationContext mutationContext) {
    // FEATURE FLAG CHECK
    if (!featureFlagService.isEnabled(NEW_DEPLOYMENT_FREEZE, mutationContext.getAccountId())) {
      throw new InvalidRequestException("Please enable feature flag to delete deployment freeze window");
    }

    String deploymentFreezeWindowId = parameter.getId();
    GovernanceConfig governanceConfig = governanceConfigService.get(mutationContext.getAccountId());
    List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigList = governanceConfig.getTimeRangeBasedFreezeConfigs();
    Boolean isDeleted = false;

    // ITERATE WITH ID CHECK
    for (TimeRangeBasedFreezeConfig t : timeRangeBasedFreezeConfigList) {
      if (t.getUuid().equals(deploymentFreezeWindowId)) {
        // DELETING THE DEPLOYMENT FREEZE WINDOW FROM THE LIST<>
        isDeleted = timeRangeBasedFreezeConfigList.remove(t);

        // UPDATING THE LIST TO GOVERNANCE CONFIG
        governanceConfig.setTimeRangeBasedFreezeConfigs(timeRangeBasedFreezeConfigList);

        // SAVING THE GOVERNANCE CONFIG
        governanceConfigService.upsert(mutationContext.getAccountId(), governanceConfig);
        break;
      }
    }

    // LOG ERROR- IF ID DOESN'T EXIST
    if (isDeleted == false) {
      throw new InvalidRequestException(
          String.format("No deployment freeze window exists with id %s", deploymentFreezeWindowId));
    }

    return QLDeleteDeploymentFreezeWindowPayload.builder().clientMutationId(parameter.getClientMutationId()).build();
  }
}
