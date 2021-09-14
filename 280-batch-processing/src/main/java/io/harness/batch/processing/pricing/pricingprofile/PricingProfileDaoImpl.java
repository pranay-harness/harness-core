/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.pricing.pricingprofile;

import io.harness.ccm.cluster.entities.PricingProfile;
import io.harness.ccm.cluster.entities.PricingProfile.PricingProfileKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.springframework.stereotype.Component;

@Component
public class PricingProfileDaoImpl implements PricingProfileDao {
  private final HPersistence hPersistence;

  @Inject
  public PricingProfileDaoImpl(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  @Override
  public PricingProfile fetchPricingProfile(String accountId) {
    return hPersistence.createQuery(PricingProfile.class).filter(PricingProfileKeys.accountId, accountId).get();
  }

  @Override
  public boolean create(PricingProfile pricingProfile) {
    return hPersistence.save(pricingProfile) != null;
  }
}
