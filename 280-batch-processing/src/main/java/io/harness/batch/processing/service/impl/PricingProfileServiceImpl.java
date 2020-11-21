package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.dao.impl.PricingProfileDaoImpl;
import io.harness.batch.processing.service.intfc.PricingProfileService;
import io.harness.ccm.cluster.entities.PricingProfile;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PricingProfileServiceImpl implements PricingProfileService {
  private PricingProfileDaoImpl pricingProfileDao;

  @Autowired
  @Inject
  public PricingProfileServiceImpl(PricingProfileDaoImpl pricingProfileDao) {
    this.pricingProfileDao = pricingProfileDao;
  }

  @Override
  public PricingProfile fetchPricingProfile(String accountId) {
    PricingProfile returnProfile = pricingProfileDao.fetchPricingProfile(accountId);
    if (returnProfile == null) {
      returnProfile =
          PricingProfile.builder().accountId(accountId).vCpuPricePerHr(0.0016).memoryGbPricePerHr(0.008).build();
    }
    return returnProfile;
  }
  @Override
  public void create(PricingProfile pricingProfile) {
    pricingProfileDao.create(pricingProfile);
  }
}
