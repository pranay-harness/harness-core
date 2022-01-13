/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.globalcontex;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;

@OwnedBy(DEL)
@Value
@Builder
public class DelegateTokenGlobalContextData implements GlobalContextData {
  public static final String TOKEN_NAME = "TOKEN_NAME";
  private String tokenName;

  @Override
  public String getKey() {
    return TOKEN_NAME;
  }
}
