package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

import software.wings.beans.marketplace.MarketPlaceType;

public class MarketplaceTypeLogContext extends AutoLogContext {
  public static final String ID = "marketPlaceType";

  public MarketplaceTypeLogContext(MarketPlaceType marketplaceType, OverrideBehavior behavior) {
    super(ID, marketplaceType.name(), behavior);
  }
}
