package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.dto.DelegateTokenInfo;

@OwnedBy(DEL)
public interface DelegateTokenAuthenticator {
  DelegateTokenInfo validateDelegateToken(String accountId, String tokenString);
}
