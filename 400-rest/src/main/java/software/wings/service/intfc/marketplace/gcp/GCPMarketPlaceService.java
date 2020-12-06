
package software.wings.service.intfc.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface GCPMarketPlaceService {
  /**
   * gets instance usage data for accountId and send usage data to GCP.
   * @param accountId - harness accountId
   */

  void createUsageReport(String accountId);
}
