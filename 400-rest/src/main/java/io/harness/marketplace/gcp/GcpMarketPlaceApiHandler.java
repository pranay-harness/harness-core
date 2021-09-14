/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.marketplace.gcp;

import static io.harness.annotations.dev.HarnessModule._940_MARKETPLACE_INTEGRATIONS;
import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import javax.ws.rs.core.Response;

@OwnedBy(GTM)
@TargetModule(_940_MARKETPLACE_INTEGRATIONS)
public interface GcpMarketPlaceApiHandler {
  /**
   * Handles POST request sent by GCP when user clicks "Register with Harness, Inc." button in GCP
   * @param token JWT token sent by GCP
   */
  Response signUp(String token);
  /**
   * Handles POST request sent by GCP when user clicks "Register with Harness, Inc." button in GCP
   * @param gcpAccountId JWT token sent by GCP
   */
  Response registerBillingOnlyTransaction(String gcpAccountId);
}
