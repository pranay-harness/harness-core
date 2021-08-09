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
