package io.harness.pricing.dto.cloudinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetailsResponse {
  @SerializedName("products") List<ProductDetails> products;

  @SerializedName("scrapingTime") String scrapingTime;
}
