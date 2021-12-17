package io.harness.subscription;

import io.harness.subscription.helpers.StripeHelper;
import io.harness.subscription.helpers.impl.StripeHelperImpl;

import com.google.inject.AbstractModule;
import com.stripe.Stripe;

public class SubscriptionSdkModule extends AbstractModule {
  private static SubscriptionSdkModule instance;
  private static SubscriptionConfig subscriptionConfig;

  private SubscriptionSdkModule() {}

  public static SubscriptionSdkModule getInstance(SubscriptionConfig config) {
    if (instance == null) {
      instance = new SubscriptionSdkModule();
      subscriptionConfig = config;

      Stripe.apiKey = subscriptionConfig.getStripeApiKey();
      Stripe.setMaxNetworkRetries(subscriptionConfig.getMaxNetworkReties());
      Stripe.setConnectTimeout(subscriptionConfig.getConnectTimeout());
      Stripe.setReadTimeout(subscriptionConfig.getReadTimeout());
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(StripeHelper.class).to(StripeHelperImpl.class);
  }
}