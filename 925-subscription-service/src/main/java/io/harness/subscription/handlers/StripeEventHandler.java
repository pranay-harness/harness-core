package io.harness.subscription.handlers;

import com.stripe.model.Event;

public interface StripeEventHandler {
  void handleEvent(Event event);
}
