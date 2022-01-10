/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
