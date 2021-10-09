package io.harness.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;

@Builder
@OwnedBy(HarnessTeam.PL)
class HackMorphiaClass implements MorphiaInterface {
  private String test;
  private String className;
}
