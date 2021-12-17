package io.harness.subscription.handlers;

import io.harness.repositories.SubscriptionDetailRepository;
import io.harness.subscription.entities.SubscriptionDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SubscriptionUpdateHandler implements StripeEventHandler {
  private final SubscriptionDetailRepository subscriptionDetailRepository;

  @Inject
  public SubscriptionUpdateHandler(SubscriptionDetailRepository subscriptionDetailRepository) {
    this.subscriptionDetailRepository = subscriptionDetailRepository;
  }

  @Override
  public void handleEvent(Event event) {
    Subscription subscription = StripeEventUtils.convertEvent(event, Subscription.class);

    log.info("Updating subscription {} from event, status {}", subscription.getId(), subscription.getStatus());

    if ("incomplete_expired".equalsIgnoreCase(subscription.getStatus())) {
      subscriptionDetailRepository.deleteBySubscriptionId(subscription.getId());
      return;
    }

    SubscriptionDetail subscriptionDetail = subscriptionDetailRepository.findBySubscriptionId(subscription.getId());
    if (subscriptionDetail != null) {
      subscriptionDetail.setStatus(subscription.getStatus());
      subscriptionDetail.setCancelAt(subscription.getCancelAt() == null ? null : subscription.getCancelAt() * 1000);
      subscriptionDetail.setCanceledAt(
          subscription.getCanceledAt() == null ? null : subscription.getCanceledAt() * 1000);
      subscriptionDetail.setLatestInvoice(subscription.getLatestInvoice());
      subscriptionDetailRepository.save(subscriptionDetail);
      log.info("synchronized subscription {} to DB", subscription.getId());
    }
    log.info("Handled subscription update for {}", subscription.getId());
  }
}
