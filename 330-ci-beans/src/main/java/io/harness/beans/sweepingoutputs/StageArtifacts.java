package io.harness.beans.sweepingoutputs;

import io.harness.annotation.RecasterAlias;
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
@RecasterAlias("io.harness.beans.sweepingoutputs.StageArtifacts")
public class StageArtifacts implements ExecutionSweepingOutput {
  @Singular List<PublishedFileArtifact> publishedFileArtifacts;
  @Singular List<PublishedImageArtifact> publishedImageArtifacts;
}
