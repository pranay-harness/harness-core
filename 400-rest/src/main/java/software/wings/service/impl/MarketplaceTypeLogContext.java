/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

import software.wings.beans.marketplace.MarketPlaceType;

public class MarketplaceTypeLogContext extends AutoLogContext {
  public static final String ID = "marketPlaceType";

  public MarketplaceTypeLogContext(MarketPlaceType marketplaceType, OverrideBehavior behavior) {
    super(ID, marketplaceType.name(), behavior);
  }
}
