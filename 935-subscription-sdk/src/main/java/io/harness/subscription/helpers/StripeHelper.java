package io.harness.subscription.helpers;

import io.harness.subscription.dto.CustomerDetail;
import io.harness.subscription.dto.InvoiceDetail;
import io.harness.subscription.dto.PriceCollectionDTO;
import io.harness.subscription.dto.SubscriptionDetail;
import io.harness.subscription.params.CustomerParams;
import io.harness.subscription.params.SubscriptionParams;

import java.util.List;

public interface StripeHelper {
  CustomerDetail createCustomer(CustomerParams customerParams);

  CustomerDetail updateCustomer(CustomerParams customerParams);

  CustomerDetail getCustomer(String customerId);

  PriceCollectionDTO listPrices(List<String> lookupKeys);
  SubscriptionDetail createSubscription(SubscriptionParams subscriptionParams);
  SubscriptionDetail updateSubscriptionQuantity(SubscriptionParams subscriptionParams);
  SubscriptionDetail updateSubscriptionPeriod(SubscriptionParams subscriptionParams);
  InvoiceDetail previewInvoice(SubscriptionParams subscriptionParams);
}
