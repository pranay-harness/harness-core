package io.harness.redesign.advisers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class HttpResponseCodeSwitchAdviserParameters {
  @Singular Map<Integer, String> responseCodeNodeIdMappings;
}
