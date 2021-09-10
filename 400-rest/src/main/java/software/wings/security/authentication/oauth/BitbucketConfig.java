package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Singleton
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class BitbucketConfig {
  private String callbackUrl;
  private String clientId;
  private String clientSecret;
}
