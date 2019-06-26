package io.harness.marketplace.gcp.signup;

import software.wings.beans.MarketPlace;

import java.net.URI;

public interface GcpMarketplaceSignUpHandler {
  /**
   * @return url to redirect to
   */
  URI signUp(MarketPlace marketPlace);
}
