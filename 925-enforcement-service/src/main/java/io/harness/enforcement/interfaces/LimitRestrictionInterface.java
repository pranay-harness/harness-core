package io.harness.enforcement.interfaces;

import io.harness.enforcement.services.impl.EnforcementSdkClient;

public interface LimitRestrictionInterface {
  Long getLimit();
  String getClientName();
  EnforcementSdkClient getEnforcementSdkClient();
  void setEnforcementSdkClient(EnforcementSdkClient enforcementSdkClient);
}
