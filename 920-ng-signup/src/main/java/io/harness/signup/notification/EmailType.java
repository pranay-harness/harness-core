/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.signup.notification;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EmailType {
  VERIFY,
  CONFIRM;

  @JsonCreator
  public static EmailType fromString(String emailType) {
    for (EmailType type : EmailType.values()) {
      if (type.name().equalsIgnoreCase(emailType)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + emailType);
  }
}
