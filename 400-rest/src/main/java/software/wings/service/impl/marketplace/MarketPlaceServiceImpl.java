/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
