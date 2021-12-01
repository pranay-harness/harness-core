package software.wings.security.authentication;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface TotpChecker<T> {
  boolean check(T request);
}
