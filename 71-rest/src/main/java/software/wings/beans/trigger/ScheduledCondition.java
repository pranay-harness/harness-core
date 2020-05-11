package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
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
