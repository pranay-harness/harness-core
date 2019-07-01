package software.wings.service.intfc.marketplace;

import software.wings.beans.MarketPlace;
import software.wings.beans.marketplace.MarketPlaceType;

import java.util.Optional;

public interface MarketPlaceService {
  /**
   * returns @link MarketPlace entity for customerIdentificationCode
   * @param customerIdentificationCode
   */
  Optional<MarketPlace> fetchMarketplace(String customerIdentificationCode, MarketPlaceType marketPlaceType);
}
