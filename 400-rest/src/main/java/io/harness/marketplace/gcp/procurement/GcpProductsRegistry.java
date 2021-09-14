/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.marketplace.gcp.procurement;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(PL)
public class GcpProductsRegistry {
  @Inject private Map<String, GcpProductHandler> gcpProductHandlers;

  public GcpProductHandler getGcpProductHandler(String productName) {
    return gcpProductHandlers.get(productName);
  }
}
