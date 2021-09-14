/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard.service;

import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/14/17
 */
@Data
@Builder
public class ServiceInstanceDashboard {
  private EntitySummary serviceSummary;
  private List<CurrentActiveInstances> currentActiveInstancesList;
  private List<DeploymentHistory> deploymentHistoryList;
}
