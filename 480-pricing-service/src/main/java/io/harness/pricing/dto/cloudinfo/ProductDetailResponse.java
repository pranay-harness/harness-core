package io.harness.pricing.dto.cloudinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDetailResponse {
  @SerializedName("product") ProductDetails product;

  @SerializedName("scrapingTime") String scrapingTime;
}
