package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("PIPELINE_SOURCE")
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class TriggerArtifactSelectionFromPipelineSource implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactSelectionType artifactSelectionType = ArtifactSelectionType.PIPELINE_SOURCE;
}
