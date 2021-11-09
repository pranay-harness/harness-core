package io.harness.service.intfc;

import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;

import java.util.List;

public interface DelegateNgTokenService {
  DelegateTokenDetails createToken(String accountId, DelegateEntityOwner owner, String name, String identifier);

  DelegateTokenDetails revokeDelegateToken(String accountId, DelegateEntityOwner owner, String tokenName);

  List<DelegateTokenDetails> getDelegateTokens(String accountId, DelegateEntityOwner owner, DelegateTokenStatus status);
}
