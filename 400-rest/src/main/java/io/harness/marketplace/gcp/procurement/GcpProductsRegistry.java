/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
