/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.marketplace.gcp.GCPBillingJobEntity;

@OwnedBy(PL)
public interface GCPBillingPollingService {
  /**
   * Create entry for scheduling GCP Job to report usage data.
   * Save an instance of {@link GCPBillingJobEntity}
   */
  String create(GCPBillingJobEntity gcpBillingJobEntity);

  /**
   * Delete schedule for accountId
   * @param accountId
   */
  void delete(String accountId);
}
