package io.harness.service.intfc;

import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenStatus;

import java.util.List;

public interface DelegateNgTokenService {
  String DEFAULT_TOKEN_NAME = "Default";

  DelegateTokenDetails createToken(String accountId, DelegateEntityOwner owner, String name);

  DelegateTokenDetails revokeDelegateToken(String accountId, DelegateEntityOwner owner, String tokenName);

  List<DelegateTokenDetails> getDelegateTokens(String accountId, DelegateEntityOwner owner, DelegateTokenStatus status);

  DelegateTokenDetails getDelegateToken(String accountId, DelegateEntityOwner owner, String name);

  String getDelegateTokenValue(String accountId, DelegateEntityOwner owner, String name);

  DelegateTokenDetails upsertDefaultToken(String accountIdentifier, DelegateEntityOwner owner, boolean skipIfExists);

  List<String> getOrgsWithActiveDefaultDelegateTokens(String accountId);

  List<String> getProjectsWithActiveDefaultDelegateTokens(String accountId);
}
