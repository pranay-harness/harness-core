/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.marketplace;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.MarketPlace;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.marketplace.MarketPlaceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@Slf4j
public class MarketPlaceServiceImpl implements MarketPlaceService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Optional<MarketPlace> fetchMarketplace(String customerIdentificationCode, MarketPlaceType marketPlaceType) {
    MarketPlace marketPlace = wingsPersistence.createQuery(MarketPlace.class)
                                  .field("type")
                                  .equal(marketPlaceType)
                                  .field("customerIdentificationCode")
                                  .equal(customerIdentificationCode)
                                  .get();

    return Optional.ofNullable(marketPlace);
  }
}
