package software.wings.beans.alert;

import io.harness.notifications.beans.Conditions;
import lombok.NonNull;
import lombok.Value;

@Value
public class AlertFilter {
  @NonNull private AlertType alertType;
  @NonNull private final Conditions conditions;
}
