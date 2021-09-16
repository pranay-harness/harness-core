package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@OwnedBy(PL)
@Data
public class SsoRedirectRequest {
  String jwtToken;
}
