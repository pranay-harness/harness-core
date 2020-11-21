package io.harness.batch.processing.service.impl;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.batch.processing.pricing.service.impl.VMPricingServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.Resource;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InstanceResourceServiceImplTest {
  @InjectMocks InstanceResourceServiceImpl instanceResourceService;
  @Mock private VMPricingServiceImpl vmPricingService;

  private static final String REGION = "asia-east2";
  private final String GCP_INSTANCE_FAMILY = "n1-standard-4";
  private final String GCP_CUSTOM_INSTANCE_FAMILY = "n2-custom-2-5120";

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetGCPCustomResource() {
    Resource computeVMResource =
        instanceResourceService.getComputeVMResource(GCP_CUSTOM_INSTANCE_FAMILY, REGION, CloudProvider.GCP);
    assertThat(computeVMResource.getCpuUnits()).isEqualTo(2048.0);
    assertThat(computeVMResource.getMemoryMb()).isEqualTo(5120.0);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetGCPResource() {
    when(vmPricingService.getComputeVMPricingInfo(GCP_INSTANCE_FAMILY, REGION, CloudProvider.GCP))
        .thenReturn(createVMComputePricingInfo());
    Resource computeVMResource =
        instanceResourceService.getComputeVMResource(GCP_INSTANCE_FAMILY, REGION, CloudProvider.GCP);
    assertThat(computeVMResource.getCpuUnits()).isEqualTo(4096.0);
    assertThat(computeVMResource.getMemoryMb()).isEqualTo(15360.0);
  }

  private VMComputePricingInfo createVMComputePricingInfo() {
    return VMComputePricingInfo.builder().cpusPerVm(4.0).memPerVm(15.0).type(GCP_INSTANCE_FAMILY).build();
  }
}
