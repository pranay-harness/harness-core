/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import io.harness.alert.AlertData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailSendingFailedAlert implements AlertData {
  private String emailAlertData;

  @Override
  public boolean matches(AlertData alertData) {
    return ((EmailSendingFailedAlert) alertData).getEmailAlertData().equals(emailAlertData);
  }

  @Override
  public String buildTitle() {
    return emailAlertData;
  }
}
