package io.harness.accesscontrol.scopes.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;

@OwnedBy(HarnessTeam.PL)
public interface ScopeParams {
  Map<String, String> getParams();
}
