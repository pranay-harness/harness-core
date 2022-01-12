/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
