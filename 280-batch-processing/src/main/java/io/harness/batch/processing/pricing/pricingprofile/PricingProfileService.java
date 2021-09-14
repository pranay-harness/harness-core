/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing.pricingprofile;

import io.harness.ccm.cluster.entities.PricingProfile;
import io.harness.ccm.commons.beans.billing.InstanceCategory;

public interface PricingProfileService {
  PricingProfile fetchPricingProfile(String accountId, InstanceCategory instanceCategory);

  void create(PricingProfile pricingProfile);
}
