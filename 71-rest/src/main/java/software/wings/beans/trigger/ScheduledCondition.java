package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@JsonTypeName("SCHEDULED")
@Value
@Builder
public class ScheduledCondition implements Condition {
  @NotEmpty private String cronExpression;
  private String cronDescription;
  @NotNull private Type type = Type.SCHEDULED;

  @Builder.Default private boolean onNewArtifactOnly = true;
  @Override
  public Type getType() {
    return type;
  }
}
