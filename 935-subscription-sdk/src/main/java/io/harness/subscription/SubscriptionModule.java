package io.harness.subscription;

import com.google.inject.AbstractModule;
import com.stripe.Stripe;

public class SubscriptionModule extends AbstractModule {
  private static SubscriptionModule instance;
  private static SubscriptionConfig subscriptionConfig;

  private SubscriptionModule() {}

  public static SubscriptionModule getInstance(SubscriptionConfig config) {
    if (instance == null) {
      instance = new SubscriptionModule();
      subscriptionConfig = config;

      Stripe.apiKey = subscriptionConfig.getStripeApiKey();
      Stripe.setMaxNetworkRetries(subscriptionConfig.getMaxNetworkReties());
      Stripe.setConnectTimeout(subscriptionConfig.getConnectTimeout());
      Stripe.setReadTimeout(subscriptionConfig.getReadTimeout());
    }
    return instance;
  }

  @Override
  protected void configure() {}
}