package io.harness.subscription.services.impl;

import io.harness.exception.InvalidRequestException;
import io.harness.subscription.constant.Prices;
import io.harness.subscription.dto.InvoiceDetailDTO;
import io.harness.subscription.dto.PriceCollectionDTO;
import io.harness.subscription.dto.PriceDTO;
import io.harness.subscription.dto.SubscriptionDTO;
import io.harness.subscription.dto.SubscriptionDetailDTO;
import io.harness.subscription.helpers.StripeHelper;
import io.harness.subscription.params.SubscriptionParams;
import io.harness.subscription.services.SubscriptionService;

import com.google.common.base.Strings;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.PriceCollection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class SubscriptionServiceImpl implements SubscriptionService {
  private final StripeHelper stripeHelper;
  //  private final LoadingCache<Prices,PriceDTO> priceCache;

  @Inject
  public SubscriptionServiceImpl(StripeHelper stripeHelper) {
    this.stripeHelper = stripeHelper;
  }

  @Override
  public Optional<PriceDTO> getPrice(Prices lookupKey) {
    PriceCollectionDTO priceCollectionDTO = listPrices(null, Lists.newArrayList(lookupKey));
    return Optional.ofNullable(priceCollectionDTO.getPrices().get(0));
  }

  @Override
  public PriceCollectionDTO listPrices(String accountIdentifier, List<Prices> lookupPrices) {
    if (lookupPrices.isEmpty()) {
      return PriceCollectionDTO.builder().prices(Lists.newArrayList()).build();
    }
    return stripeHelper.listPrices(lookupPrices.stream().map(p -> p.name()).collect(Collectors.toList()));
  }

  @Override
  public PriceCollection listPrices(String accountIdentifier, Map<String, Object> params) {
    try {
      return Price.list(params);
    } catch (StripeException e) {
      throw new InvalidRequestException("Failed to list prices", e);
    }
  }

  @Override
  public InvoiceDetailDTO previewInvoice(SubscriptionDTO subscriptionDTO) {
    // Find customerId and subscriptionId from DB by accountId and moduleType
    // if there is one set subscriptionId and customerId
    // otherwise set nothing.
    String customerId = "cus_KjdpeBBusw5Mrk";
    String subscriptionId = "sub_1K5CC0Iqk5P9Eha3XvbVzseX";

    SubscriptionParams params = SubscriptionParams.builder().build();
    params.setItems(subscriptionDTO.getItems());
    // either subscriptionId or customerId should be provided
    params.setCustomerId(customerId);
    params.setSubscriptionId(subscriptionId);
    return stripeHelper.previewInvoice(params);
  }

  @Override
  public SubscriptionDetailDTO createSubscription(SubscriptionDTO subscriptionDTO) {
    // Check if customerId and subscriptionId exists from DB by accountId and moduleType
    // if subscriptionId exists, not allowed for creationg

    // if customerId not exists, create customer in stripe

    // create Subscription
    SubscriptionParams param = SubscriptionParams.builder()
                                   .accountIdentifier(subscriptionDTO.getAccountIdentifier())
                                   .customerId("cus_KjdpeBBusw5Mrk")
                                   .items(subscriptionDTO.getItems())
                                   .build();
    return stripeHelper.createSubscription(param);
  }

  @Override
  public SubscriptionDetailDTO updateSubscription(SubscriptionDTO subscriptionDTO) {
    // check in subscription id
    validateUpdateSubscriptionRequest(subscriptionDTO);
    SubscriptionParams param = SubscriptionParams.builder()
                                   .accountIdentifier(subscriptionDTO.getAccountIdentifier())
                                   .customerId("cus_KjdpeBBusw5Mrk")
                                   .items(subscriptionDTO.getItems())
                                   .build();
    return stripeHelper.updateSubscriptionPeriod(param);
  }

  @Override
  public SubscriptionDetailDTO cancelSubscription(SubscriptionDTO subscriptionDTO) {
    return null;
  }

  private void validateUpdateSubscriptionRequest(SubscriptionDTO subscriptionDTO) {
    if (Strings.isNullOrEmpty(subscriptionDTO.getSubscriptionId())) {
      throw new InvalidRequestException("Invalid subscriptionId");
    }
  }
}
