package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface ApplicationAccess {
  String getAppId();
}
