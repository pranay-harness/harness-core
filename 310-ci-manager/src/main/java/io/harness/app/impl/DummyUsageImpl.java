package io.harness.app.impl;

import io.harness.ModuleType;
import io.harness.ci.usage.beans.CILicenseUsageDTO;
import io.harness.licensing.usage.beans.LicenseUsageDTO;
import io.harness.licensing.usage.beans.ReferenceDTO;
import io.harness.licensing.usage.beans.UsageDataDTO;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

@Singleton
public class DummyUsageImpl implements LicenseUsageInterface {
  @Override
  public LicenseUsageDTO getLicenseUsage(String accountIdentifier, ModuleType module, long timestamp) {
    return CILicenseUsageDTO.builder()
        .accountIdentifier(accountIdentifier)
        .module(module.name())
        .timestamp(timestamp)
        .activeCommitters(UsageDataDTO.builder()
                              .count(2)
                              .displayName("active in last 60 days")
                              .references(Lists.newArrayList(ReferenceDTO.builder()
                                                                 .accountIdentifier(accountIdentifier)
                                                                 .identifier("1")
                                                                 .orgIdentifier("2")
                                                                 .projectIdentifier("3")
                                                                 .build(),
                                  ReferenceDTO.builder().accountIdentifier(accountIdentifier).identifier("a").build()))
                              .build())
        .build();
  }
}
