package io.harness.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface ExpansionKeysConstants {
  String CONNECTOR_EXPANSION_KEY = "connector";
  String SERVICE_EXPANSION_KEY = "service";
  String ENV_EXPANSION_KEY = "environment";
}
