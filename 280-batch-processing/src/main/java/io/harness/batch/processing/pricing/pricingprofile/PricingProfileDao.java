/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing.pricingprofile;

import io.harness.ccm.cluster.entities.PricingProfile;

public interface PricingProfileDao {
  boolean create(PricingProfile pricingProfile);

  PricingProfile fetchPricingProfile(String accountId);
}
