package io.harness.state.io.ambiance;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Redesign
@Value
@Builder
public class Ambiance {
  // Setup details accountId, appId
  @Singular Map<String, String> setupAbstractions;

  // These is a combination of setup/execution Id for a particular level
  @Singular Map<String, Level> levels;
}