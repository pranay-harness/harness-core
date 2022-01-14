package io.harness.enforcement.interfaces;

import io.harness.enforcement.services.impl.EnforcementSdkClient;

public interface EnforcementSdkSupportInterface {
  void setEnforcementSdkClient(EnforcementSdkClient enforcementSdkClient);
  String getClientName();
  EnforcementSdkClient getEnforcementSdkClient();
}
