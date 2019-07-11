package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class ArtifactCondition implements Condition {
  @NotEmpty private String artifactStreamId;
  @NotEmpty private transient String artifactSourceName;
  @NotEmpty private transient String artifactStreamType;
  private String artifactFilter;
  @NotNull private Type type = Type.NEW_ARTIFACT;

  @Override
  public Type getType() {
    return type;
  }
}
