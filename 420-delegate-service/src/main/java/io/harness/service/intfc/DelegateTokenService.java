/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
