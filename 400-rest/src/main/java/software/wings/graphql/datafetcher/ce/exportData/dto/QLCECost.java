/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLCECost {
  TOTALCOST("billingamount"),
  IDLECOST("idlecost"),
  UNALLOCATEDCOST("unallocatedcost"),
  NETWORKCOST("networkcost"),
  SYSTEMCOST("systemcost"),
  MEMORYCOST("memorybillingamount"),
  MEMORYIDLECOST("memoryIdleCost"),
  MEMORYUNALLOCATEDCOST("memoryunallocatedcost"),
  CPUCOST("cpubillingamount"),
  CPUIDLECOST("cpuIdleCost"),
  CPUUNALLOCATEDCOST("cpuunallocatedcost"),
  STORAGECOST("storagecost"),
  STORAGEIDLECOST("storageactualidlecost"),
  STORAGEUNALLOCATEDCOST("storageunallocatedcost");

  private final String name;

  QLCECost(String s) {
    name = s;
  }

  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }

  public String toString() {
    return this.name;
  }
}
