package io.harness.beans.sweepingoutputs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CI)
public class CISweepingOutputNames {
  public static final String STAGE_ARTIFACT_SWEEPING_OUTPUT_NAME = "integrationStageArtifacts";
  public static final String CODE_BASE_CONNECTOR_REF = "codeBaseConnectorRef";
  public static final String CODEBASE = "codebase";
}
