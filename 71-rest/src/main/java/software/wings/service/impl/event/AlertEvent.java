package software.wings.service.impl.event;

import io.harness.event.model.EventInfo;
import lombok.Value;
import software.wings.beans.alert.Alert;

@Value
public class AlertEvent implements EventInfo {
  private Alert alert;
}
