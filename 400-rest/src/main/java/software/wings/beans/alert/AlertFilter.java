package software.wings.beans.alert;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.notifications.beans.Conditions;

import lombok.NonNull;
import lombok.Value;

@OwnedBy(PL)
@Value
public class AlertFilter {
  @NonNull private AlertType alertType;
  @NonNull private final Conditions conditions;
}
