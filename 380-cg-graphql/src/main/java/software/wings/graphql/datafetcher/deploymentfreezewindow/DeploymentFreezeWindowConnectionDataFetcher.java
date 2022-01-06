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
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.type.deploymentfreezewindow.QLDeploymentFreezeWindow;
import software.wings.graphql.schema.type.deploymentfreezewindow.QLDeploymentFreezeWindowConnection;
import software.wings.graphql.schema.type.deploymentfreezewindow.QLDeploymentFreezeWindowConnectionQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class DeploymentFreezeWindowConnectionDataFetcher
    extends AbstractObjectDataFetcher<QLDeploymentFreezeWindowConnection,
        QLDeploymentFreezeWindowConnectionQueryParameters> {
  @Inject GovernanceConfigService governanceConfigService;
  @Inject DeploymentFreezeWindowController deploymentFreezeWindowController;
  @Inject FeatureFlagService featureFlagService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  public QLDeploymentFreezeWindowConnection fetch(
      QLDeploymentFreezeWindowConnectionQueryParameters parameter, String accountId) {
    // FEATURE FLAG CHECK
    if (!featureFlagService.isEnabled(NEW_DEPLOYMENT_FREEZE, accountId)) {
      throw new InvalidRequestException("Please enable feature flag to list the deployment freeze window");
    }

    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigList = governanceConfig.getTimeRangeBasedFreezeConfigs();
    List<QLDeploymentFreezeWindow> filterList = new ArrayList<>();

    Boolean getEnabled = parameter.getListEnabled();

    // LIST ALL -> IF NULL
    if (getEnabled == null) {
      for (TimeRangeBasedFreezeConfig t : timeRangeBasedFreezeConfigList) {
        QLDeploymentFreezeWindow qlDeploymentFreezeWindow =
            deploymentFreezeWindowController.populateDeploymentFreezeWindowPayload(t);
        filterList.add(qlDeploymentFreezeWindow);
      }
    } else {
      for (TimeRangeBasedFreezeConfig t : timeRangeBasedFreezeConfigList) {
        // LIST ENABLED -> IF TRUE , LIST DISABLED -> IF FALSE
        if (t.isApplicable() == getEnabled) {
          QLDeploymentFreezeWindow qlDeploymentFreezeWindow =
              deploymentFreezeWindowController.populateDeploymentFreezeWindowPayload(t);
          filterList.add(qlDeploymentFreezeWindow);
        }
      }
    }

    // OFFSET
    Integer offset = 1;
    if (parameter.getOffset() != null) {
      if (parameter.getOffset() > 0) {
        offset = parameter.getOffset();
      } else {
        throw new InvalidRequestException(
            "'offset' parameter must be a positive integer. Please enter a valid 'offset' parameter.");
      }
    }

    // LIMIT
    Integer limit;
    if (parameter.getLimit() != null) {
      if (parameter.getLimit() > 0) {
        limit = parameter.getLimit();
      } else {
        throw new InvalidRequestException(
            "'limit' parameter must be a positive integer. Please enter a valid 'limit' parameter.");
      }
    } else {
      throw new InvalidRequestException("'limit' parameter cannot be empty.");
    }

    if (offset > filterList.size()) {
      throw new InvalidRequestException("'offset' parameter is greater than the total list size.");
    }

    List<QLDeploymentFreezeWindow> finalList = new ArrayList<>();
    for (int i = offset; i < offset + limit && i <= filterList.size(); i++) {
      finalList.add(filterList.get(i - 1));
    }

    // CREATING PAYLOAD
    return QLDeploymentFreezeWindowConnection.builder().nodes(finalList).build();
  }
}
