/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.marketplace.gcp.procurement;

public interface GcpProductHandler {
  void handleNewSubscription(String accountId, String plan);

  void handlePlanChange(String accountId, String newPlan);

  void handleSubscriptionCancellation(String accountId);
}
