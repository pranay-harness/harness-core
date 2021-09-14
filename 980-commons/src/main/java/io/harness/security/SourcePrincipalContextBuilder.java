/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
