package io.harness.marketplace.gcp.procurement;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;

@OwnedBy(PL)
public class GcpProductsRegistry {
  @Inject private Map<String, GcpProductHandler> gcpProductHandlers;

  public GcpProductHandler getGcpProductHandler(String productName) {
    return gcpProductHandlers.get(productName);
  }
}
