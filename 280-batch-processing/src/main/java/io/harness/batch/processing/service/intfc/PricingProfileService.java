package io.harness.batch.processing.service.intfc;

import io.harness.ccm.cluster.entities.PricingProfile;

public interface PricingProfileService {
  PricingProfile fetchPricingProfile(String accountId);

  void create(PricingProfile pricingProfile);
}
