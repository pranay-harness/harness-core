package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.InstanceCategory;
import io.harness.ccm.cluster.entities.PricingProfile;

public interface PricingProfileService {
  PricingProfile fetchPricingProfile(String accountId, InstanceCategory instanceCategory);

  void create(PricingProfile pricingProfile);
}
