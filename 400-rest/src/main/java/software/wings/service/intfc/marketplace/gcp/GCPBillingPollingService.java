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
