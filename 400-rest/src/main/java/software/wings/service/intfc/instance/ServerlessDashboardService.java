/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.instance;

import io.harness.beans.PageResponse;

import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.instance.dashboard.InstanceStatsByEnvironment;
import software.wings.beans.instance.dashboard.InstanceSummaryStats;
import software.wings.beans.instance.dashboard.InstanceSummaryStatsByService;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hibernate.validator.constraints.NotEmpty;

public interface ServerlessDashboardService {
  InstanceSummaryStats getAppInstanceSummaryStats(
      @NotEmpty String accountId, List<String> appIds, List<String> groupByEntityTypes, long timestamp);

  PageResponse<InstanceSummaryStatsByService> getAppInstanceSummaryStatsByService(
      String accountId, List<String> appIds, long timestamp, int offset, int limit);

  List<InstanceStatsByEnvironment> getServiceInstances(String accountId, String serviceId, long timestamp);
  ServerlessInstance getInstanceDetails(String instanceId);
  @Nonnull List<ServerlessInstance> getAppInstancesForAccount(@NotEmpty String accountId, long timestamp);
  Set<String> getDeletedAppIds(String accountId, long fromTimestamp, long toTimestamp);
}
