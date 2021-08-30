package io.harness.batch.processing.pricing.banzai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class PricingResponse {
  private List<VMComputePricingInfo> products;
}
