package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@JsonTypeName("NEW_ARTIFACT")
@Value
@Builder
public class ArtifactCondition implements Condition {
  @NotEmpty private String artifactStreamId;
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
