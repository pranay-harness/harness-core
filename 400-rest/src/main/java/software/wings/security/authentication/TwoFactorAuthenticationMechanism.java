package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(PL) @TargetModule(_950_NG_AUTHENTICATION_SERVICE) public enum TwoFactorAuthenticationMechanism { TOTP }
