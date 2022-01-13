/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import software.wings.beans.ServiceInstance;
import software.wings.sm.StepExecutionSummary;

import java.util.List;

/**
 * Created by rishi on 4/4/17.
 */
public class SelectNodeStepExecutionSummary extends StepExecutionSummary {
  private List<ServiceInstance> serviceInstanceList;
  private boolean excludeSelectedHostsFromFuturePhases;

  public List<ServiceInstance> getServiceInstanceList() {
    return serviceInstanceList;
  }

  public void setServiceInstanceList(List<ServiceInstance> serviceInstanceList) {
    this.serviceInstanceList = serviceInstanceList;
  }

  public boolean isExcludeSelectedHostsFromFuturePhases() {
    return excludeSelectedHostsFromFuturePhases;
  }

  public void setExcludeSelectedHostsFromFuturePhases(boolean excludeSelectedHostsFromFuturePhases) {
    this.excludeSelectedHostsFromFuturePhases = excludeSelectedHostsFromFuturePhases;
  }
}
