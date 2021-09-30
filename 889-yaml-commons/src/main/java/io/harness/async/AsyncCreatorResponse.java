package io.harness.async;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.Dependencies;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface AsyncCreatorResponse {
  Dependencies getDependencies();

  List<String> getErrorMessages();

  void setDependencies(Dependencies dependencies);
}
