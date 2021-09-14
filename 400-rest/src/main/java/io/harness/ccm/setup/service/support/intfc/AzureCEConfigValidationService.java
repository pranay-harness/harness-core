/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup.service.support.intfc;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ce.CEAzureConfig;

@OwnedBy(CE)
public interface AzureCEConfigValidationService {
  void verifyCrossAccountAttributes(CEAzureConfig ceAzureConfig);
}
