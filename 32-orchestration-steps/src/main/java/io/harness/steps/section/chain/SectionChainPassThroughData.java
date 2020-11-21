package io.harness.steps.section.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;

import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
public class SectionChainPassThroughData implements PassThroughData {
  int childIndex;
}
