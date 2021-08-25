package io.harness.beans.steps.outcome;

import static io.harness.beans.steps.outcome.CIOutcomeNames.INTEGRATION_STAGE_OUTCOME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.PublishedFileArtifact;
import io.harness.beans.execution.PublishedImageArtifact;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias(INTEGRATION_STAGE_OUTCOME)
@JsonTypeName(INTEGRATION_STAGE_OUTCOME)
@OwnedBy(HarnessTeam.CI)
@RecasterAlias("io.harness.beans.steps.outcome.IntegrationStageOutcome")
public class IntegrationStageOutcome implements Outcome {
  @Singular Set<PublishedImageArtifact> imageArtifacts;
  @Singular Set<PublishedFileArtifact> fileArtifacts;
}
