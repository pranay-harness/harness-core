package io.harness.redesign.advisers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.AdviserParameters;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class HttpResponseCodeSwitchAdviserParameters implements AdviserParameters {
  @Singular Map<Integer, String> responseCodeNodeIdMappings;
}