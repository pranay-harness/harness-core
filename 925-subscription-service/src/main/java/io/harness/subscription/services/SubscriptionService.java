package io.harness.subscription.services;

import io.harness.subscription.constant.Prices;
import io.harness.subscription.dto.InvoiceDetailDTO;
import io.harness.subscription.dto.PriceCollectionDTO;
import io.harness.subscription.dto.PriceDTO;
import io.harness.subscription.dto.SubscriptionDTO;
import io.harness.subscription.dto.SubscriptionDetailDTO;

import com.stripe.model.PriceCollection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SubscriptionService {
  Optional<PriceDTO> getPrice(Prices lookupKey);
  PriceCollectionDTO listPrices(String accountIdentifier, List<Prices> lookupPrices);
  PriceCollection listPrices(String accountIdentifier, Map<String, Object> params);
  InvoiceDetailDTO previewInvoice(SubscriptionDTO subscriptionDTO);
  SubscriptionDetailDTO createSubscription(SubscriptionDTO subscriptionDTO);
  SubscriptionDetailDTO updateSubscription(SubscriptionDTO subscriptionDTO);
  SubscriptionDetailDTO cancelSubscription(SubscriptionDTO subscriptionDTO);
}
