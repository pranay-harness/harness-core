package software.wings.service.impl.marketplace;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.MarketPlace;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.marketplace.MarketPlaceService;

import java.util.Optional;

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
