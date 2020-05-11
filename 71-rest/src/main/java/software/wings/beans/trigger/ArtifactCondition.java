package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@JsonTypeName("NEW_ARTIFACT")
@Value
@Builder
public class ArtifactCondition implements Condition {
  @NotEmpty private String artifactStreamId;
  private String artifactServerId;
  private transient String artifactServerName;
  private transient String artifactStreamName;
  private transient String artifactStreamType;
  private String artifactFilter;
  @NotNull private Type type = Type.NEW_ARTIFACT;

  @Override
  public Type getType() {
    return type;
  }
}
