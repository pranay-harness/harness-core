package io.harness.pricing.dto.cloudinfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZonePrice {
  @SerializedName("price") Double price;

  @SerializedName("zone") String zone;
}