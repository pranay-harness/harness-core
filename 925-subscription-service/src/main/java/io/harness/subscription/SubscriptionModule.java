package io.harness.subscription;

import io.harness.subscription.services.SubscriptionService;
import io.harness.subscription.services.impl.SubscriptionServiceImpl;

import com.google.inject.AbstractModule;

public class SubscriptionModule extends AbstractModule {
  private static SubscriptionModule instance;
  private SubscriptionConfig subscriptionConfig;

  public static SubscriptionModule getInstance(SubscriptionConfig subscriptionConfig) {
    if (instance == null) {
      instance = new SubscriptionModule(subscriptionConfig);
    }
    return instance;
  }

  private SubscriptionModule(SubscriptionConfig subscriptionConfig) {
    this.subscriptionConfig = subscriptionConfig;
  }

  @Override
  protected void configure() {
    install(SubscriptionSdkModule.getInstance(subscriptionConfig));
    bind(SubscriptionService.class).to(SubscriptionServiceImpl.class);
  }
}