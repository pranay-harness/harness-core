package io.harness.subscription.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceDTO {
  private String priceId;
  private boolean isActive;
  private String currency;
  private List<TiersDTO> tiersDTO;
  private TierMode tierMode;
  private Long unitAmount;
}
