package io.harness.batch.processing.pricing.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VMComputePricingInfo {
  private String category;
  private String type;
  private double onDemandPrice;
  private List<ZonePrice> spotPrice;
  private double networkPrice;
  private double cpusPerVm;
  private double memPerVm;
}
