package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secret.ConfigSecret;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Singleton
public class MarketPlaceConfig {
  @ConfigSecret private String awsAccessKey;
  @ConfigSecret private String awsSecretKey;
  @ConfigSecret private String awsMarketPlaceProductCode;
  @ConfigSecret private String awsMarketPlaceCeProductCode;
  @ConfigSecret private String azureMarketplaceAccessKey;
  @ConfigSecret private String azureMarketplaceSecretKey;
}
