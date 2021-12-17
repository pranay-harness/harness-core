package io.harness.subscription;

import io.harness.subscription.handlers.InvoicePaymentSucceedHandler;
import io.harness.subscription.handlers.StripeEventHandler;
import io.harness.subscription.handlers.SubscriptionDeleteHandler;
import io.harness.subscription.handlers.SubscriptionUpdateHandler;
import io.harness.subscription.services.SubscriptionService;
import io.harness.subscription.services.impl.SubscriptionServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

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

    MapBinder<String, StripeEventHandler> eventHandlerMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StripeEventHandler.class);
    eventHandlerMapBinder.addBinding("invoice.payment_succeeded").to(InvoicePaymentSucceedHandler.class);
    eventHandlerMapBinder.addBinding("customer.subscription.updated").to(SubscriptionUpdateHandler.class);
    eventHandlerMapBinder.addBinding("customer.subscription.deleted").to(SubscriptionDeleteHandler.class);
  }
}