package io.harness.filters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@OwnedBy(HarnessTeam.PIPELINE)
public class DummyGenericStepFilterCreator extends GenericStepPMSFilterJsonCreator {
  @Override
  public Set<String> getSupportedStepTypes() {
    return null;
  }
}
