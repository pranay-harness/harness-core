package io.harness.plancreator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PipelineServiceUtilPlanCreationConstants {
  public final String COMBINED_ROLLBACK_ID_SUFFIX = "_combinedRollback";
}
