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
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.governance.TimeRangeBasedFreezeConfig;

import software.wings.beans.governance.GovernanceConfig;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.type.deploymentfreezewindow.QLDeploymentFreezeWindow;
import software.wings.graphql.schema.type.deploymentfreezewindow.QLDeploymentFreezeWindowQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class DeploymentFreezeWindowDataFetcher
    extends AbstractObjectDataFetcher<QLDeploymentFreezeWindow, QLDeploymentFreezeWindowQueryParameters> {
  @Inject GovernanceConfigService governanceConfigService;
  @Inject DeploymentFreezeWindowController deploymentFreezeWindowController;
  @Inject FeatureFlagService featureFlagService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  public QLDeploymentFreezeWindow fetch(QLDeploymentFreezeWindowQueryParameters parameter, String accountId) {
    // FEATURE FLAG CHECK
    if (!featureFlagService.isEnabled(NEW_DEPLOYMENT_FREEZE, accountId)) {
      throw new InvalidRequestException("Please enable feature flag to fetch deployment freeze window");
    }

    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigList = governanceConfig.getTimeRangeBasedFreezeConfigs();
    TimeRangeBasedFreezeConfig getDeploymentFreezeWindow = null;

    // GET BY ID
    String inputId = parameter.getId();
    if (inputId != null) {
      if (EmptyPredicate.isEmpty(inputId)) {
        throw new InvalidRequestException("'id' parameter cannot be empty.");
      }
      for (TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig : timeRangeBasedFreezeConfigList) {
        if (timeRangeBasedFreezeConfig.getUuid().equals(parameter.getId())) {
          getDeploymentFreezeWindow = timeRangeBasedFreezeConfig;
          break;
        }
      }
    }

    // GET BY NAME
    String inputName = parameter.getName();
    if (inputName != null) {
      if (EmptyPredicate.isEmpty(inputName)) {
        throw new InvalidRequestException("'name' parameter cannot be empty.");
      }
      for (TimeRangeBasedFreezeConfig timeRangeBasedFreezeConfig : timeRangeBasedFreezeConfigList) {
        if (timeRangeBasedFreezeConfig.getName().equals(parameter.getName())) {
          getDeploymentFreezeWindow = timeRangeBasedFreezeConfig;
          break;
        }
      }
    }

    if (getDeploymentFreezeWindow == null) {
      throw new InvalidRequestException("Deployment Freeze Window does not exist");
    }

    // POPULATING THE PAYLOAD
    return deploymentFreezeWindowController.populateDeploymentFreezeWindowPayload(getDeploymentFreezeWindow);
  }
}
