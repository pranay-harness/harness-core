package io.harness.state.io.ambiance;

import io.harness.annotations.Redesign;

import java.util.Map;

@Redesign
public interface Ambiance {
  // Setup details accountId, appId
  Map<String, String> getSetupAbstractions();

  // These is a combination of setup/execution Id for a particular level
  Map<String, Level> getLevels();
}