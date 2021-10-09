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
@JsonTypeName("LAST_DEPLOYED")
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class TriggerArtifactSelectionLastDeployed implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactSelectionType artifactSelectionType = ArtifactSelectionType.LAST_DEPLOYED;
  @NotEmpty private String id;
  private transient String name;
  @NotEmpty private TriggerLastDeployedType type;
}
