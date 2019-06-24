package software.wings.service.intfc.marketplace.gcp;

import software.wings.beans.marketplace.gcp.GCPBillingJobEntity;

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
