package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class ArtifactCondition implements Condition {
  @NotEmpty private String artifactStreamId;
  private String artifactSourceName;
  private String artifactFilter;
  @NotNull private Type type;
}
