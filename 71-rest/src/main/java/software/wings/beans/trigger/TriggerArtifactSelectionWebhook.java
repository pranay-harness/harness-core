package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("WEBHOOK_VARIABLE")
public class TriggerArtifactSelectionWebhook implements TriggerArtifactSelectionValue {
  @NotEmpty private ArtifactSelectionType artifactSelectionType = ArtifactSelectionType.WEBHOOK_VARIABLE;
  @NotEmpty private String artifactStreamId;
  @NotEmpty private String artifactServerId;
  private transient String artifactStreamName;
  private transient String artifactServerName;
  private transient String artifactStreamType;
  private String artifactFilter;
}