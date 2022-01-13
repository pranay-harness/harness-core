/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.instance;

import static io.harness.govern.Switch.unhandled;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.persistence.HPersistence;

import software.wings.beans.instance.dashboard.InstanceStatsUtils;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLInstancesCountQueryParameters;
import software.wings.graphql.schema.type.QLInstanceCount;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class InstanceCountDataFetcher
    extends AbstractObjectDataFetcher<QLInstanceCount, QLInstancesCountQueryParameters> {
  @Inject DashboardStatisticsService dashboardService;
  @Inject InstanceStatService instanceStatService;
  @Inject AppService appService;
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLInstanceCount fetch(QLInstancesCountQueryParameters qlQuery, String accountId) {
    switch (qlQuery.getInstanceCountType()) {
      case NINETY_FIVE_PERCENTILE:
        return QLInstanceCount.builder()
            .count((int) (InstanceStatsUtils.actualUsage(qlQuery.getAccountId(), instanceStatService)))
            .instanceCountType(qlQuery.getInstanceCountType())
            .build();

      case TOTAL:
        List<String> appIds = appService.getAppIdsByAccountId(qlQuery.getAccountId());
        return QLInstanceCount.builder()
            .count((int) (dashboardService.getTotalInstancesForAccount(qlQuery.getAccountId(), appIds)))
            .instanceCountType(qlQuery.getInstanceCountType())
            .build();

      default:
        unhandled(qlQuery.getInstanceCountType());
    }
    return null;
  }
}
