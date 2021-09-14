/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources.alert;

import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;

import lombok.Value;

/**
 * Just a wrapper model over {@link software.wings.beans.alert.AlertType} enum so that Jackson serializes getters to
 * fields.
 */
@Value
class AlertType {
  private software.wings.beans.alert.AlertType alertType;

  public AlertCategory getCategory() {
    return alertType.getCategory();
  }

  public AlertSeverity getSeverity() {
    return alertType.getSeverity();
  }
}
