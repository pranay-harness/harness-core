package io.harness.beans.sweepingoutputs;

import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.STAGE_ARTIFACT_SWEEPING_OUTPUT_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.PublishedFileArtifact;
import io.harness.beans.execution.PublishedImageArtifact;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@OwnedBy(HarnessTeam.CI)
public class StageArtifacts implements ExecutionSweepingOutput {
  @Singular List<PublishedFileArtifact> publishedFileArtifacts;
  @Singular List<PublishedImageArtifact> publishedImageArtifacts;

  @Override
  public String getType() {
    return STAGE_ARTIFACT_SWEEPING_OUTPUT_NAME;
  }
}
