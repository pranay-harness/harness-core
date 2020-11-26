package io.harness.steps.section.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@TypeAlias("sectionChainPassThroughData")
public class SectionChainPassThroughData implements PassThroughData {
  int childIndex;
}
