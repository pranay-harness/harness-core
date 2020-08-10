package io.harness.batch.processing.dao.impl;

import static io.harness.rule.OwnerRule.SANDESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.PricingProfile;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class PricingProfileDaoImplTest extends WingsBaseTest {
  @Inject private PricingProfileDaoImpl pricingProfileDaoImpl;

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  public void shouldCreate() {
    PricingProfile defaultProfile =
        PricingProfile.builder().accountId("default").vCpuPricePerHr(0.0016).memoryGbPricePerHr(0.008).build();
    assertThat(pricingProfileDaoImpl.create(defaultProfile)).isEqualTo(true);
  }

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  public void shouldFetch() {
    PricingProfile defaultProfile =
        PricingProfile.builder().accountId("default").vCpuPricePerHr(0.0016).memoryGbPricePerHr(0.008).build();
    pricingProfileDaoImpl.create(defaultProfile);
    PricingProfile defaultPricingProfile = pricingProfileDaoImpl.fetchPricingProfile(defaultProfile.getAccountId());
    assertThat(defaultPricingProfile.getAccountId()).isEqualTo(defaultProfile.getAccountId());
    assertThat(defaultPricingProfile.getVCpuPricePerHr()).isEqualTo(defaultProfile.getVCpuPricePerHr());
    assertThat(defaultPricingProfile.getMemoryGbPricePerHr()).isEqualTo(defaultProfile.getMemoryGbPricePerHr());
  }
}