package io.harness.subscription.helpers;

import io.harness.subscription.dto.CustomerDetailDTO;
import io.harness.subscription.dto.InvoiceDetailDTO;
import io.harness.subscription.dto.PriceCollectionDTO;
import io.harness.subscription.dto.SubscriptionDetailDTO;
import io.harness.subscription.params.CustomerParams;
import io.harness.subscription.params.SubscriptionParams;

import java.util.List;

public interface StripeHelper {
  CustomerDetailDTO createCustomer(CustomerParams customerParams);

  CustomerDetailDTO updateCustomer(CustomerParams customerParams);

  CustomerDetailDTO getCustomer(String customerId);

  PriceCollectionDTO listPrices(List<String> lookupKeys);
  SubscriptionDetailDTO createSubscription(SubscriptionParams subscriptionParams);
  SubscriptionDetailDTO updateSubscriptionQuantity(SubscriptionParams subscriptionParams);
  SubscriptionDetailDTO updateSubscriptionPeriod(SubscriptionParams subscriptionParams);
  InvoiceDetailDTO previewInvoice(SubscriptionParams subscriptionParams);
}
