package io.harness.marketplace.gcp.procurement;

public interface GcpProductHandler {
  void handleNewSubscription(String accountId, String plan);

  void handlePlanChange(String accountId, String newPlan);

  void handleSubscriptionCancellation(String accountId);
}
