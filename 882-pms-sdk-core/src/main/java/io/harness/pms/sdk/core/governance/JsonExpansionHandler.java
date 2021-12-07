package io.harness.pms.sdk.core.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.databind.JsonNode;

@OwnedBy(PIPELINE)
public interface JsonExpansionHandler {
  ExpansionResponse expand(JsonNode fieldValue);
}
