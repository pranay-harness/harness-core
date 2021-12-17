package io.harness.subscription.helpers.impl;

import io.harness.exception.InvalidArgumentsException;
import io.harness.subscription.dto.CustomerDetailDTO;
import io.harness.subscription.dto.InvoiceDetailDTO;
import io.harness.subscription.dto.ItemDTO;
import io.harness.subscription.dto.PriceCollectionDTO;
import io.harness.subscription.dto.PriceDTO;
import io.harness.subscription.dto.SubscriptionDetailDTO;
import io.harness.subscription.dto.TierMode;
import io.harness.subscription.dto.TiersDTO;
import io.harness.subscription.helpers.StripeHelper;
import io.harness.subscription.params.CustomerParams;
import io.harness.subscription.params.ItemParams;
import io.harness.subscription.params.SubscriptionParams;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.InvoiceUpcomingParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class StripeHelperImpl implements StripeHelper {
  private StripeHandlerImpl stripeHandler;
  private List<String> subscriptionExpandList = Arrays.asList("latest_invoice.payment_intent");

  public StripeHelperImpl() {
    this.stripeHandler = new StripeHandlerImpl();
  }

  @Override
  public CustomerDetailDTO createCustomer(CustomerParams customerParams) {
    CustomerCreateParams params = CustomerCreateParams.builder()
                                      .setEmail(customerParams.getBillingContactEmail())
                                      .setName(customerParams.getName())
                                      .setMetadata(ImmutableMap.of("accountId", customerParams.getAccountIdentifier()))
                                      .build();
    Customer customer = stripeHandler.createCustomer(params);
    return toCustomerDetail(customer);
  }

  @Override
  public CustomerDetailDTO updateCustomer(CustomerParams customerParams) {
    if (Strings.isNullOrEmpty(customerParams.getCustomerId())) {
      throw new InvalidArgumentsException("Customer id is missing");
    }
    CustomerUpdateParams.Builder paramsBuilder = CustomerUpdateParams.builder();
    if (!Strings.isNullOrEmpty(customerParams.getAccountIdentifier())) {
      paramsBuilder.setName(customerParams.getName());
    }
    if (!Strings.isNullOrEmpty(customerParams.getBillingContactEmail())) {
      paramsBuilder.setEmail(customerParams.getBillingContactEmail());
    }
    if (!Strings.isNullOrEmpty(customerParams.getAccountIdentifier())) {
      paramsBuilder.setMetadata(ImmutableMap.of("accountId", customerParams.getAccountIdentifier()));
    }
    Customer customer = stripeHandler.updateCustomer(customerParams.getCustomerId(), paramsBuilder.build());
    return toCustomerDetail(customer);
  }

  @Override
  public CustomerDetailDTO getCustomer(String customerId) {
    return toCustomerDetail(stripeHandler.retrieveCustomer(customerId));
  }

  @Override
  public PriceCollectionDTO listPrices(List<String> lookupKeys) {
    PriceListParams params = PriceListParams.builder()
                                 .setActive(true)
                                 .addAllLookupKeys(lookupKeys)
                                 .addAllExpand(Lists.newArrayList("data.tiers"))
                                 .build();
    PriceCollection priceCollection = stripeHandler.listPrices(params);
    return toPriceCollectionDTO(priceCollection);
  }

  @Override
  public SubscriptionDetailDTO createSubscription(SubscriptionParams subscriptionParams) {
    SubscriptionCreateParams.Builder creationParamsBuilder = SubscriptionCreateParams.builder();
    creationParamsBuilder.setCustomer(subscriptionParams.getCustomerId())
        .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
        .addAllExpand(subscriptionExpandList);

    // Register subscription items
    subscriptionParams.getItems().forEach(item
        -> creationParamsBuilder.addItem(SubscriptionCreateParams.Item.builder()
                                             .setPrice(item.getPriceId())
                                             .setQuantity(item.getQuantity())
                                             .build()));
    Subscription subscription = stripeHandler.createSubscription(creationParamsBuilder.build());
    return toSubscriptionDetail(subscription);
  }

  @Override
  public SubscriptionDetailDTO updateSubscriptionQuantity(SubscriptionParams subscriptionParams) {
    Subscription subscription = stripeHandler.retrieveSubscription(subscriptionParams.getSubscriptionId());

    // Collect item information in new subscription
    Map<String, ItemParams> newItems = new HashMap<>();
    subscriptionParams.getItems().forEach(item -> newItems.put(item.getPriceId(), item));

    // Go through current subscription and update.
    SubscriptionUpdateParams.Builder updateParamBuilder = SubscriptionUpdateParams.builder();
    updateParamBuilder.setCancelAtPeriodEnd(false).setProrationBehavior(
        SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS);
    if (!newItems.isEmpty()) {
      List<SubscriptionItem> data = subscription.getItems().getData();
      for (SubscriptionItem item : data) {
        if (newItems.containsKey(item.getPrice().getId())) {
          updateParamBuilder.addItem(SubscriptionUpdateParams.Item.builder()
                                         .setId(item.getId())
                                         .setPrice(newItems.get(item.getPrice().getId()).getPriceId())
                                         .setQuantity(newItems.get(item.getPrice().getId()).getQuantity())
                                         .build());
        }
      }
    }
    return toSubscriptionDetail(
        stripeHandler.updateSubscription(subscriptionParams.getSubscriptionId(), updateParamBuilder.build()));
  }

  @Override
  public SubscriptionDetailDTO updateSubscriptionPeriod(SubscriptionParams subscriptionParams) {
    Subscription subscription = stripeHandler.retrieveSubscription(subscriptionParams.getSubscriptionId());

    // Collect item information in new subscription
    Map<String, ItemParams> newItems = new HashMap<>();
    subscriptionParams.getItems().forEach(item -> newItems.put(item.getPriceId(), item));

    // Go through current subscription and update.
    SubscriptionUpdateParams.Builder updateParamBuilder = SubscriptionUpdateParams.builder();
    updateParamBuilder.setCancelAtPeriodEnd(false).setProrationBehavior(
        SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS);
    if (!newItems.isEmpty()) {
      List<SubscriptionItem> data = subscription.getItems().getData();
      for (SubscriptionItem item : data) {
        if (newItems.containsKey(item.getPrice().getId())) {
          updateParamBuilder.addItem(SubscriptionUpdateParams.Item.builder()
                                         .setId(item.getId())
                                         .setPrice(newItems.get(item.getPrice().getId()).getPriceId())
                                         .setQuantity(newItems.get(item.getPrice().getId()).getQuantity())
                                         .build());
        }
      }
    }
    return toSubscriptionDetail(
        stripeHandler.updateSubscription(subscriptionParams.getSubscriptionId(), updateParamBuilder.build()));
  }

  @Override
  public InvoiceDetailDTO previewInvoice(SubscriptionParams subscriptionParams) {
    InvoiceUpcomingParams.Builder upcomingParamBuilder = InvoiceUpcomingParams.builder();

    if (!Strings.isNullOrEmpty(subscriptionParams.getCustomerId())) {
      upcomingParamBuilder.setCustomer(subscriptionParams.getCustomerId());
    }

    if (!Strings.isNullOrEmpty(subscriptionParams.getSubscriptionId())) {
      Subscription subscription = stripeHandler.retrieveSubscription(subscriptionParams.getSubscriptionId());

      // Delete existed items in subscription
      List<SubscriptionItem> data = subscription.getItems().getData();
      data.forEach(oldItem
          -> upcomingParamBuilder.addSubscriptionItem(
              InvoiceUpcomingParams.SubscriptionItem.builder().setId(oldItem.getId()).setDeleted(true).build()));
      upcomingParamBuilder.setSubscription(subscriptionParams.getSubscriptionId());
    }

    // Add new items
    subscriptionParams.getItems().forEach(newItem
        -> upcomingParamBuilder.addSubscriptionItem(InvoiceUpcomingParams.SubscriptionItem.builder()
                                                        .setPrice(newItem.getPriceId())
                                                        .setQuantity(newItem.getQuantity())
                                                        .build()));

    return toInvoiceDetail(stripeHandler.previewInvoice(
        subscriptionParams.getCustomerId(), subscriptionParams.getSubscriptionId(), upcomingParamBuilder.build()));
  }

  private InvoiceDetailDTO toInvoiceDetail(Invoice invoice) {
    InvoiceDetailDTO dto = InvoiceDetailDTO.builder()
                               .subscriptionId(invoice.getSubscription())
                               .totalAmount(invoice.getTotal())
                               .amountDue(invoice.getAmountDue())
                               .periodEnd(invoice.getPeriodEnd())
                               .periodStart(invoice.getPeriodStart())
                               .nextPaymentAttempt(invoice.getNextPaymentAttempt())
                               .items(new ArrayList<>())
                               .build();
    for (InvoiceLineItem item : invoice.getLines().getData()) {
      ItemDTO itemDTO = ItemDTO.builder()
                            .amount(item.getAmount())
                            .quantity(item.getQuantity())
                            .description(item.getDescription())
                            .build();
      dto.getItems().add(itemDTO);
    }

    return dto;
  }

  private CustomerDetailDTO toCustomerDetail(Customer customer) {
    return CustomerDetailDTO.builder().build();
  }

  private PriceCollectionDTO toPriceCollectionDTO(PriceCollection priceCollection) {
    PriceCollectionDTO priceCollectionDTO = PriceCollectionDTO.builder().prices(new ArrayList<>()).build();

    List<Price> data = priceCollection.getData();
    for (Price price : data) {
      priceCollectionDTO.getPrices().add(toPriceDTO(price));
    }
    return priceCollectionDTO;
  }

  private PriceDTO toPriceDTO(Price price) {
    PriceDTO priceDTO = PriceDTO.builder()
                            .productId(price.getProduct())
                            .lookupKey(price.getLookupKey())
                            .isActive(price.getActive())
                            .currency(price.getCurrency())
                            .priceId(price.getId())
                            .build();

    if (Strings.isNullOrEmpty(price.getTiersMode())) {
      priceDTO.setUnitAmount(price.getUnitAmount());
    } else {
      priceDTO.setTierMode(TierMode.fromString(price.getTiersMode()));
      priceDTO.setTiersDTO(price.getTiers()
                               .stream()
                               .map(t -> TiersDTO.builder().unitAmount(t.getUnitAmount()).upTo(t.getUpTo()).build())
                               .collect(Collectors.toList()));
    }
    return priceDTO;
  }

  private SubscriptionDetailDTO toSubscriptionDetail(Subscription subscription) {
    return SubscriptionDetailDTO.builder()
        .subscriptionId(subscription.getId())
        .clientSecret(subscription.getLatestInvoiceObject().getPaymentIntentObject().getClientSecret())
        .build();
  }
}
