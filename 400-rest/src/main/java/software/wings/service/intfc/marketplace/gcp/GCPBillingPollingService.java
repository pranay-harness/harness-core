/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
