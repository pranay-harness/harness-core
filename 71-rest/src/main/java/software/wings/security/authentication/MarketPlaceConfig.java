package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Singleton
public class MarketPlaceConfig {
  private String awsAccessKey;
  private String awsSecretKey;
  private String awsMarketPlaceProductCode;
  private String azureMarketplaceAccessKey;
  private String azureMarketplaceSecretKey;
}
