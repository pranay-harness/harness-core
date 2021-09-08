package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
@OwnedBy(PL)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class AzureConfig {
  private String clientId;
  private String clientSecret;
  private String callbackUrl;
}
