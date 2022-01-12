/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
