package io.harness.subscription.caches.impl;

import io.harness.subscription.caches.PriceCache;
import io.harness.subscription.constant.Prices;
import io.harness.subscription.dto.PriceCollectionDTO;
import io.harness.subscription.dto.PriceDTO;
import io.harness.subscription.helpers.StripeHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class PriceCacheImpl implements PriceCache {
  private final StripeHelper stripeHelper;
  private Map<Prices, PriceDTO> priceCache;

  @Inject
  public PriceCacheImpl(StripeHelper stripeHelper) {
    this.stripeHelper = stripeHelper;
    this.priceCache = new HashMap<>();
  }

  @Override
  public PriceDTO getPrice(Prices price) {
    if (priceCache.isEmpty()) {
      reloadAll();
    }
    return priceCache.get(price);
  }

  @Override
  public void reloadAll() {
    PriceCollectionDTO priceCollectionDTO =
        stripeHelper.listPrices(Arrays.stream(Prices.values()).map(p -> p.name()).collect(Collectors.toList()));
    priceCollectionDTO.getPrices().forEach(
        priceDTO -> priceCache.put(Prices.valueOf(priceDTO.getLookupKey()), priceDTO));
  }
}
