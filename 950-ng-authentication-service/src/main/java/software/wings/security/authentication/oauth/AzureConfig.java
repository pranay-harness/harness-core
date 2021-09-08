package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
@OwnedBy(PL)
public class AzureConfig {
  private String clientId;
  private String clientSecret;
  private String callbackUrl;
}
