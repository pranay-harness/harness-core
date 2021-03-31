package io.harness.ccm.setup.service.support.intfc;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ce.CEAzureConfig;

@OwnedBy(CE)
public interface AzureCEConfigValidationService {
  void verifyCrossAccountAttributes(CEAzureConfig ceAzureConfig);
}
