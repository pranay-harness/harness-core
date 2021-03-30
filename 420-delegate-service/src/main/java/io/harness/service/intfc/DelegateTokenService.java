package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateToken;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateTokenService {
  DelegateToken createDelegateToken(String accountId, String tokenName);

  void revokeDelegateToken(String accountId, String tokenName);

  void deleteDelegateToken(String accountId, String tokenName);

  String getTokenValue(String accountId, String tokenName);
}
