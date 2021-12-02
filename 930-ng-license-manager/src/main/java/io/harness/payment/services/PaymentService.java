package io.harness.payment.services;

import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;

public interface PaymentService {
  CustomerRecord createCustomer(CustomerRecord customerRecord);
  CustomerRecord updateCustomer(CustomerRecord customerRecord);
  CustomerRecord deleteCustomer(String customerId);
  Customer getCustomerRecord(String customerId);

  Subscription createSubscription(String customerId, SubscriptionRecord subscription);
  Subscription updateSubscription(String subscriptionId, SubscriptionRecord subscription);
  void cancelSubscription(String subscriptionId);
  Subscription retrieveSubscription(String subscriptionId);
  Invoice previewInvoice(String subscriptionId, SubscriptionRecord subscriptionRecord);
}
