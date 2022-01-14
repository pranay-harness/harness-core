package io.harness.steps.section.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@TypeAlias("sectionChainPassThroughData")
@RecasterAlias("io.harness.steps.section.chain.SectionChainPassThroughData")
public class SectionChainPassThroughData implements PassThroughData {
  int childIndex;
}
