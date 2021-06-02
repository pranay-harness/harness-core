package io.harness.batch.processing.pricing.service.impl;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.pricing.client.BanzaiPricingClient;
import io.harness.batch.processing.pricing.data.EcsFargatePricingInfo;
import io.harness.batch.processing.pricing.data.PricingResponse;
import io.harness.batch.processing.pricing.data.VMComputePricingInfo;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import retrofit2.Call;
import retrofit2.Response;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class VMPricingServiceImplTest extends CategoryTest {
  @InjectMocks private VMPricingServiceImpl vmPricingService;
  @Mock private BanzaiPricingClient banzaiPricingClient;
  private final String REGION = "us-east-1";
  private final String COMPUTE_SERVICE = "compute";
  private final String DEFAULT_INSTANCE_FAMILY = "c4.8xlarge";
  private final double DEFAULT_INSTANCE_CPU = 36;
  private final double DEFAULT_INSTANCE_MEMORY = 60;
  private final double DEFAULT_INSTANCE_PRICE = 1.60;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetComputeVMPricingInfo() throws IOException {
    Call<PricingResponse> pricingInfoCall = mock(Call.class);
    when(pricingInfoCall.execute()).thenReturn(createPricingResponse());
    when(banzaiPricingClient.getPricingInfo(CloudProvider.AWS.getCloudProviderName(), COMPUTE_SERVICE, REGION))
        .thenReturn(pricingInfoCall);
    VMComputePricingInfo computeVMPricingInfo =
        vmPricingService.getComputeVMPricingInfo(DEFAULT_INSTANCE_FAMILY, REGION, CloudProvider.AWS);
    assertThat(computeVMPricingInfo).isNotNull();
    assertThat(computeVMPricingInfo.getCpusPerVm()).isEqualTo(DEFAULT_INSTANCE_CPU);
    assertThat(computeVMPricingInfo.getMemPerVm()).isEqualTo(DEFAULT_INSTANCE_MEMORY);
    assertThat(computeVMPricingInfo.getOnDemandPrice()).isEqualTo(DEFAULT_INSTANCE_PRICE);
    assertThat(computeVMPricingInfo.getType()).isEqualTo(DEFAULT_INSTANCE_FAMILY);
    VMComputePricingInfo computeVMPricingInfoCached =
        vmPricingService.getComputeVMPricingInfo(DEFAULT_INSTANCE_FAMILY, REGION, CloudProvider.AWS);
    assertThat(computeVMPricingInfoCached).isNotNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetComputeVMPricingInfoAzure() throws IOException {
    Call<PricingResponse> pricingInfoCall = mock(Call.class);
    when(pricingInfoCall.execute()).thenReturn(createPricingResponse());
    when(banzaiPricingClient.getPricingInfo(CloudProvider.AZURE.getCloudProviderName(), COMPUTE_SERVICE, "uksouth"))
        .thenReturn(pricingInfoCall);
    VMComputePricingInfo computeVMPricingInfo =
        vmPricingService.getComputeVMPricingInfo(DEFAULT_INSTANCE_FAMILY, "germanywestcentral", CloudProvider.AZURE);
    assertThat(computeVMPricingInfo).isNotNull();
    assertThat(computeVMPricingInfo.getCpusPerVm()).isEqualTo(DEFAULT_INSTANCE_CPU);
    assertThat(computeVMPricingInfo.getMemPerVm()).isEqualTo(DEFAULT_INSTANCE_MEMORY);
    assertThat(computeVMPricingInfo.getOnDemandPrice()).isEqualTo(DEFAULT_INSTANCE_PRICE);
    assertThat(computeVMPricingInfo.getType()).isEqualTo(DEFAULT_INSTANCE_FAMILY);
    VMComputePricingInfo computeVMPricingInfoCached =
        vmPricingService.getComputeVMPricingInfo(DEFAULT_INSTANCE_FAMILY, "germanywestcentral", CloudProvider.AZURE);
    assertThat(computeVMPricingInfoCached).isNotNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetCustomComputeVMPricingInfo() throws IOException {
    VMComputePricingInfo computeVMPricingInfo =
        vmPricingService.getComputeVMPricingInfo("n2-standard-16", REGION, CloudProvider.GCP);
    assertThat(computeVMPricingInfo).isNotNull();
    assertThat(computeVMPricingInfo.getCpusPerVm()).isEqualTo(16.0);
    assertThat(computeVMPricingInfo.getMemPerVm()).isEqualTo(64.0);
    assertThat(computeVMPricingInfo.getOnDemandPrice()).isEqualTo(0.7769);
    assertThat(computeVMPricingInfo.getType()).isEqualTo("n2-standard-16");
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testShouldReturnNullVMPricingInfo() throws IOException {
    Call<PricingResponse> pricingInfoCall = mock(Call.class);
    when(pricingInfoCall.execute()).thenThrow(IOException.class);
    when(banzaiPricingClient.getPricingInfo(CloudProvider.AWS.getCloudProviderName(), COMPUTE_SERVICE, REGION))
        .thenReturn(pricingInfoCall);
    VMComputePricingInfo computeVMPricingInfo =
        vmPricingService.getComputeVMPricingInfo(DEFAULT_INSTANCE_FAMILY, REGION, CloudProvider.AWS);
    assertThat(computeVMPricingInfo).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetFargatePricingInfo() {
    EcsFargatePricingInfo fargatePricingInfo = vmPricingService.getFargatePricingInfo(REGION);
    assertThat(fargatePricingInfo).isNotNull();
    assertThat(fargatePricingInfo.getCpuPrice()).isGreaterThan(0d);
    assertThat(fargatePricingInfo.getMemoryPrice()).isGreaterThan(0d);
  }

  private Response createPricingResponse() {
    VMComputePricingInfo vmComputePricingInfo = VMComputePricingInfo.builder()
                                                    .cpusPerVm(DEFAULT_INSTANCE_CPU)
                                                    .memPerVm(DEFAULT_INSTANCE_MEMORY)
                                                    .onDemandPrice(DEFAULT_INSTANCE_PRICE)
                                                    .type(DEFAULT_INSTANCE_FAMILY)
                                                    .build();
    PricingResponse pricingResponse = new PricingResponse(Arrays.asList(vmComputePricingInfo));
    return Response.success(pricingResponse);
  }
}
