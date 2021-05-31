package io.harness.batch.processing.service.intfc;

import io.harness.ccm.cluster.entities.PricingProfile;
import io.harness.ccm.commons.beans.billing.InstanceCategory;

public interface PricingProfileService {
  PricingProfile fetchPricingProfile(String accountId, InstanceCategory instanceCategory);

  void create(PricingProfile pricingProfile);
}
