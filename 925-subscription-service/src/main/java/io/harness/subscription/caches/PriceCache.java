package io.harness.subscription.caches;

import io.harness.subscription.constant.Prices;
import io.harness.subscription.dto.PriceDTO;

public interface PriceCache {
  PriceDTO getPrice(Prices prices);
  void reloadAll();
}
