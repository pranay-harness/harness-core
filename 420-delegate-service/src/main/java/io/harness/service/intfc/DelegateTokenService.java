package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;

import java.util.List;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateTokenService {
  DelegateTokenDetails createDelegateToken(String accountId, String tokenName);

  DelegateTokenDetails upsertDefaultToken(String accountId, String tokenValue);

  void revokeDelegateToken(String accountId, String tokenName);

  void deleteDelegateToken(String accountId, String tokenName);

  String getTokenValue(String accountId, String tokenName);

  List<DelegateTokenDetails> getDelegateTokens(String accountId, DelegateTokenStatus status, String tokenName);
}
