package io.harness.batch.processing.pricing.banzai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
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
