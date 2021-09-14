/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.marketplace;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.MarketPlace;
import software.wings.beans.marketplace.MarketPlaceType;

import java.util.Optional;

@OwnedBy(PL)
public interface MarketPlaceService {
  /**
   * returns @link MarketPlace entity for customerIdentificationCode
   * @param customerIdentificationCode
   */
  Optional<MarketPlace> fetchMarketplace(String customerIdentificationCode, MarketPlaceType marketPlaceType);
}
