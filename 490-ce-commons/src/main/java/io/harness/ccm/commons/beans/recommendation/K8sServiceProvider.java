/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.beans.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.billing.InstanceCategory;
import io.harness.ccm.commons.constants.CloudProvider;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
public class K8sServiceProvider {
  String region;
  String instanceFamily;
  int nodeCount;

  CloudProvider cloudProvider;
  InstanceCategory instanceCategory;

  double cpusPerVm;
  double memPerVm;
  double costPerVmPerHr;
  double spotCostPerVmPerHr;

  public double getCategoryAwareCost() {
    if (InstanceCategory.SPOT.equals(instanceCategory)) {
      return spotCostPerVmPerHr;
    }
    return costPerVmPerHr;
  }
}
