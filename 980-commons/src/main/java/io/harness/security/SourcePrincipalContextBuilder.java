/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager;
import io.harness.security.dto.Principal;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class SourcePrincipalContextBuilder {
  public void setSourcePrincipal(Principal principal) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(SourcePrincipalContextData.builder().principal(principal).build());
  }

  public Principal getSourcePrincipal() {
    SourcePrincipalContextData sourcePrincipalContextData =
        GlobalContextManager.get(SourcePrincipalContextData.SOURCE_PRINCIPAL);
    if (sourcePrincipalContextData == null) {
      return null;
    }
    return sourcePrincipalContextData.getPrincipal();
  }
}
