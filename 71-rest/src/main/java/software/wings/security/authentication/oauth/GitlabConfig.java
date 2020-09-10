package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Singleton
public class GitlabConfig {
  private String callbackUrl;
  private String clientId;
  private String clientSecret;
}
