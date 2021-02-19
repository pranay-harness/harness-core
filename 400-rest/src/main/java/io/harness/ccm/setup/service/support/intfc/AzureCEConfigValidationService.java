package io.harness.ccm.setup.service.support.intfc;

import software.wings.beans.ce.CEAzureConfig;

public interface AzureCEConfigValidationService {
  void verifyCrossAccountAttributes(CEAzureConfig ceAzureConfig);
}
