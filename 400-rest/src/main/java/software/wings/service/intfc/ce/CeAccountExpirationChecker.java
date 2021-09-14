/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.ce;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import javax.annotation.ParametersAreNonnullByDefault;

@TargetModule(HarnessModule._490_CE_COMMONS)
@OwnedBy(CE)
@ParametersAreNonnullByDefault
public interface CeAccountExpirationChecker {
  void checkIsCeEnabled(String accountId);
}
