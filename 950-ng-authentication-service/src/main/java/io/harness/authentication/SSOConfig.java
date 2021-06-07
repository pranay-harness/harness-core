package io.harness.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.sso.SSOSettings;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class SSOConfig {
  private String accountId;
  private List<SSOSettings> ssoSettings;
  private AuthenticationMechanism authenticationMechanism;
}
