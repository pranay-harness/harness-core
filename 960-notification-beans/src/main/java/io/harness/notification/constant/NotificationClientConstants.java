package io.harness.notification.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationClientConstants {
  public static final String RESERVED_PREFIX = "reserved";
  public static final String HARNESS_NAME = "Harness";
  public static final String EMAILID_KEY = RESERVED_PREFIX + "_emailId";
  public static final String TEMPLATEID_KEY = RESERVED_PREFIX + "_templateId";
  public static final String MESSAGE_KEY = RESERVED_PREFIX + "_message";
}
