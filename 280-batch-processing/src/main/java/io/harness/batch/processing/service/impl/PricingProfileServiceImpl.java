package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.dao.impl.PricingProfileDaoImpl;
import io.harness.batch.processing.service.intfc.PricingProfileService;
import io.harness.ccm.cluster.entities.PricingProfile;
import io.harness.ccm.commons.beans.billing.InstanceCategory;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PricingProfileServiceImpl implements PricingProfileService {
  private final PricingProfileDaoImpl pricingProfileDao;

  @Autowired
  @Inject
  public PricingProfileServiceImpl(PricingProfileDaoImpl pricingProfileDao) {
    this.pricingProfileDao = pricingProfileDao;
  }

  @Override
  public PricingProfile fetchPricingProfile(String accountId, InstanceCategory instanceCategory) {
    PricingProfile returnProfile = pricingProfileDao.fetchPricingProfile(accountId);
    if (returnProfile == null) {
      double cpuPricePerHr = 0.0016;
      double memoryPricePerHr = 0.008;
      if (instanceCategory == InstanceCategory.SPOT) {
        cpuPricePerHr = 0.00064;
        memoryPricePerHr = 0.0032;
      }
      returnProfile = PricingProfile.builder()
                          .accountId(accountId)
                          .vCpuPricePerHr(cpuPricePerHr)
                          .memoryGbPricePerHr(memoryPricePerHr)
                          .build();
    }
    return returnProfile;
  }
  @Override
  public void create(PricingProfile pricingProfile) {
    pricingProfileDao.create(pricingProfile);
  }
}
