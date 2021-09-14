/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.ServiceInstanceStatistics;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface StatisticsService {
  DeploymentStatistics getDeploymentStatistics(@NotNull String accountId, List<String> appIds, int numOfDays);

  ServiceInstanceStatistics getServiceInstanceStatistics(@NotNull String accountId, List<String> appIds, int numOfDays);
}
