package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@JsonTypeName("LAST_DEPLOYED")
public class TriggerArtifactSelectionLastDeployed implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactSelectionType artifactSelectionType = ArtifactSelectionType.LAST_DEPLOYED;
  @NotEmpty private String id;
  private transient String name;
  @NotEmpty private TriggerLastDeployedType type;
}
