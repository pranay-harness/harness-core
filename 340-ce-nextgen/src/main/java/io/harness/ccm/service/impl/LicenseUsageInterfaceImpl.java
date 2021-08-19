package io.harness.ccm.service.impl;

import io.harness.ModuleType;
import io.harness.ccm.commons.beans.usage.CELicenseUsageDTO;
import io.harness.licensing.usage.beans.UsageDataDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;

public class LicenseUsageInterfaceImpl implements LicenseUsageInterface<CELicenseUsageDTO> {
  @Override
  public CELicenseUsageDTO getLicenseUsage(String accountIdentifier, ModuleType module, long timestamp) {
    return CELicenseUsageDTO.builder()
        .activeSpend(UsageDataDTO.builder().count(300000).displayName("Last Year").build())
        .timestamp(timestamp)
        .accountIdentifier(accountIdentifier)
        .build();
  }
}