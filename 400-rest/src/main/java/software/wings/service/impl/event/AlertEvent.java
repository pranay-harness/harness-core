package software.wings.service.impl.event;

import io.harness.event.model.EventInfo;

import software.wings.beans.alert.Alert;

import lombok.Value;

@Value
public class AlertEvent implements EventInfo {
  private Alert alert;
}
