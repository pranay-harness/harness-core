package io.harness.batch.processing.pricing.client;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class AppSpotPricingClientTest extends CategoryTest {
  /**
   * this unit test makes a network call,
   * which might fail when the host is not accessible via network.
   * This test is necessary to make sure that the HTTP API is working and it's format is unchanged.
   */
  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  @Ignore("API 404, the API is offline currently, ignoring till correct fix")
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