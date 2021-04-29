package io.harness.licensing.interfaces.clients.local;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.UpdateChannel;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.stats.CFRuntimUsageDTO;
import io.harness.licensing.interfaces.clients.CFModuleLicenseClient;

import com.google.common.collect.Lists;

public class CFLocalClient implements CFModuleLicenseClient {
  @Override
  public CFModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    return CFModuleLicenseDTO.builder()
        .numberOfUsers(2)
        .numberOfClientMAUs(5000)
        .updateChannels(Lists.newArrayList(UpdateChannel.POLLING))
        .build();
  }

  @Override
  public CFRuntimUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}
