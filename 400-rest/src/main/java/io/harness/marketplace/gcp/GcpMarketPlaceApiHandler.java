package io.harness.marketplace.gcp;

import javax.ws.rs.core.Response;

public interface GcpMarketPlaceApiHandler {
  /**
   * Handles POST request sent by GCP when user clicks "Register with Harness, Inc." button in GCP
   * @param token JWT token sent by GCP
   */
  Response signUp(String token);
}
