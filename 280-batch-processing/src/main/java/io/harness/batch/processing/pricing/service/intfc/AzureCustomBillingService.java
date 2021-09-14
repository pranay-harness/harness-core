/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.pricing.vmpricing.VMInstanceBillingData;
import io.harness.ccm.commons.entities.batch.InstanceData;

import java.time.Instant;
import java.util.List;

@OwnedBy(HarnessTeam.CE)
public interface AzureCustomBillingService {
  VMInstanceBillingData getComputeVMPricingInfo(InstanceData instanceData, Instant startTime, Instant endTime);

  void updateAzureVMBillingDataCache(List<String> resourceIds, Instant startTime, Instant endTime, String dataSetId);
}
