package io.harness.batch.processing.pricing.client;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CE)
@RunWith(MockitoJUnitRunner.class)
public class AppSpotPricingClientTest extends CategoryTest {
  /**
   * this unit test makes a network call,
   * even if the network call fails, the ApiClient successfully returns the response from saved resourceFile
   * 280-batch-processing/src/main/resources/pricingdata/storagePricingData.txt
   */
  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldMakeSuccessfulCall() throws IOException {
    AppSpotPricingClient.ApiResponse apiResponse = AppSpotPricingClient.fetchParsedResponse();
    assertThat(apiResponse).isNotNull();
    assertThat(apiResponse.skus).isNotEmpty();
    assertThat(apiResponse.skus.get(0)).isNotNull().isInstanceOf(AppSpotPricingClient.Sku.class);
    assertThat(apiResponse.skus.get(0).skus).isNotEmpty();
    assertThat(apiResponse.skus.get(0).skus.get(0)).isNotNull().isInstanceOf(AppSpotPricingClient.Sku.class);
    assertThat(apiResponse.skus.get(0).skus.get(0).description).isNotBlank();
    assertThat(apiResponse.skus.get(0).skus.get(0).prices).isNotEmpty();
    assertThat(apiResponse.skus.get(0).skus.get(0).service_regions).isNotEmpty();
    assertThat(apiResponse.skus.get(0).skus.get(0).skus).isNull();
  }
}