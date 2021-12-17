package io.harness.subscription.dto;

import io.harness.subscription.constant.Prices;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ListPricesDTO {
  private List<Prices> prices;
}
