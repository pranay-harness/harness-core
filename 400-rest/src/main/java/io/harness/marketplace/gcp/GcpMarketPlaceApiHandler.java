/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
