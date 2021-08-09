package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DEL)
public interface DelegateTokenAuthenticator {
  void validateDelegateToken(String accountId, String tokenString);
}
