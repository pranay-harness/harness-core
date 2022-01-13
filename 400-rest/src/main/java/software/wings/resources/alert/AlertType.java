/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
