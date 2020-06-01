package io.harness.state.core.section.chain;

import io.harness.facilitator.PassThroughData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SectionChainPassThroughData implements PassThroughData {
  int childIndex;
}
