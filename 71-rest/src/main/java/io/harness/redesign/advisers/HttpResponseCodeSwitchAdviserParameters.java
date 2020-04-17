package io.harness.redesign.advisers;

import io.harness.adviser.AdviserParameters;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
@Redesign
public class HttpResponseCodeSwitchAdviserParameters implements AdviserParameters {
  @Singular Map<Integer, String> responseCodeNodeIdMappings;
}