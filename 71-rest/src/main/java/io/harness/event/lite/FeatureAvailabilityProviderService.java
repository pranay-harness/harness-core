package io.harness.event.lite;

import java.util.List;

public interface FeatureAvailabilityProviderService {
  /**
   * Lists if certain features are available for given account or not.
   * @param accountId
   * @return
   */
  List<FeatureAvailability> listFeatureAvailability(String accountId);
}
