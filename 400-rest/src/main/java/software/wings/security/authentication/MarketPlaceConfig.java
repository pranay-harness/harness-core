package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Singleton
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class MarketPlaceConfig {
  private String awsAccessKey;
  private String awsSecretKey;
  private String awsMarketPlaceProductCode;
  private String awsMarketPlaceCeProductCode;
  private String azureMarketplaceAccessKey;
  private String azureMarketplaceSecretKey;
}
