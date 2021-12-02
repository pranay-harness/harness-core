package io.harness.payment.services.impl;

import io.harness.payment.services.CustomerRecord;
import io.harness.payment.services.PaymentService;
import io.harness.payment.services.SubscriptionItem;
import io.harness.payment.services.SubscriptionRecord;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.InvoiceUpcomingParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class PaymentServiceimpl implements PaymentService {
  public PaymentServiceimpl() {
    Stripe.apiKey =
        "sk_test_51IykZ0Iqk5P9Eha3lDEdilJNRufZDw6xXt79RT39C5RhwDLAV2IZvvSsdqZV8q9HvENsVmAnc1MITqrLap8ElwxH00EJp5orGK";
  }

  @Override
  public CustomerRecord createCustomer(CustomerRecord customerRecord) {
    CustomerCreateParams params = CustomerCreateParams.builder()
                                      .setEmail(customerRecord.getEmail())
                                      .setName(customerRecord.getCustomerName())
                                      .setMetadata(ImmutableMap.of("accountId", customerRecord.getAccountId()))
                                      .build();

    try {
      Customer customer = Customer.create(params);
      return CustomerRecord.builder()
          .id(customer.getId())
          .accountId(customer.getMetadata().get("accountId"))
          .customerName(customer.getName())
          .email(customer.getEmail())
          .build();
    } catch (StripeException e) {
      log.error("Create stripe customer failed", e);
    }

    return null;
  }

  @Override
  public CustomerRecord updateCustomer(CustomerRecord customerRecord) {
    try {
      Customer customer = Customer.retrieve(customerRecord.getId());
      Customer updatedCustomer = customer.update(ImmutableMap.of("name", customerRecord.getCustomerName()));
      return customerRecord;
    } catch (StripeException e) {
      log.error("Update stripe customer failed", e);
    }
    return null;
  }

  @Override
  public CustomerRecord deleteCustomer(String customerId) {
    try {
      Customer customer = Customer.retrieve(customerId);
      Customer delete = customer.delete();

      return CustomerRecord.builder()
          .id(delete.getId())
          .accountId(delete.getMetadata().get("accountId"))
          .customerName(delete.getName())
          .email(delete.getEmail())
          .build();
    } catch (StripeException e) {
      log.error("Delete stripe custoemr failed", e);
    }

    return null;
  }

  @Override
  public Customer getCustomerRecord(String customerId) {
    try {
      return Customer.retrieve(customerId);
    } catch (StripeException e) {
      log.error("Delete stripe custoemr failed", e);
    }
    return null;
  }

  @Override
  public Subscription createSubscription(String customerId, SubscriptionRecord subscription) {
    SubscriptionCreateParams.Builder subscriptionCreateParams =
        SubscriptionCreateParams.builder()
            .setCustomer(customerId)
            .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
            .addAllExpand(Arrays.asList("latest_invoice.payment_intent"));
    for (SubscriptionItem entry : subscription.getItems()) {
      subscriptionCreateParams.addItem(SubscriptionCreateParams.Item.builder()
                                           .setPrice(entry.getPriceId())
                                           .setQuantity(entry.getQuantity())
                                           .build());
    }
    try {
      return Subscription.create(subscriptionCreateParams.build());
    } catch (StripeException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Subscription updateSubscription(String subscriptionId, SubscriptionRecord subscriptionRecord) {
    try {
      Subscription subscription = Subscription.retrieve(subscriptionId);

      SubscriptionUpdateParams.Builder subscriptionUpdateParams = SubscriptionUpdateParams.builder();
      subscriptionUpdateParams.addItem(SubscriptionUpdateParams.Item.builder()
                                           .setId(subscription.getItems().getData().get(0).getId())
                                           .setPrice(subscriptionRecord.getItems().get(0).getPriceId())
                                           .setQuantity(subscriptionRecord.getItems().get(0).getQuantity())
                                           .build());

      return subscription.update(subscriptionUpdateParams.build());
    } catch (StripeException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void cancelSubscription(String subscriptionId) {
    try {
      Subscription subscription = Subscription.retrieve(subscriptionId);

      subscription.cancel();
    } catch (StripeException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Subscription retrieveSubscription(String subscriptionId) {
    try {
      return Subscription.retrieve(subscriptionId);
    } catch (StripeException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public Invoice previewInvoice(String subscriptionId, SubscriptionRecord subscriptionRecord) {
    try {
      Subscription subscription = Subscription.retrieve(subscriptionId);

      InvoiceUpcomingParams invoiceParams =
          InvoiceUpcomingParams.builder()
              .setCustomer(subscription.getCustomer())
              .setSubscription(subscription.getId())
              .addSubscriptionItem(InvoiceUpcomingParams.SubscriptionItem.builder()
                                       .setId(subscription.getItems().getData().get(0).getId())
                                       .setDeleted(true)
                                       .build())
              .addSubscriptionItem(InvoiceUpcomingParams.SubscriptionItem.builder()
                                       .setPrice(subscriptionRecord.getItems().get(0).getPriceId())
                                       .setQuantity(subscriptionRecord.getItems().get(0).getQuantity())
                                       .build())
              //              .addSubscriptionItem(InvoiceUpcomingParams.SubscriptionItem.builder()
              //                                       .setId(subscription.getItems().getData().get(1).getId())
              //                                       .setDeleted(true)
              //                                       .build())
              //              .addSubscriptionItem(InvoiceUpcomingParams.SubscriptionItem.builder()
              //                                       .setPrice(subscriptionRecord.getItems().get(1).getPriceId())
              //                                       .setQuantity(subscriptionRecord.getItems().get(1).getQuantity())
              //                                       .build())
              .build();

      return Invoice.upcoming(invoiceParams);

    } catch (StripeException e) {
      log.error("Failed to preview:", e);
    }
    return null;
  }
}
