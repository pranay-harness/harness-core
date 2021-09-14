/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security.authentication.recaptcha;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;
import org.slf4j.helpers.MessageFormatter;

@OwnedBy(PL)
@Value
public class MaxLoginAttemptExceededException extends Exception {
  private int limit;
  private int attempts;

  @Override
  public String getMessage() {
    return MessageFormatter.format("Login Attempts. limit={} attempts={}", limit, attempts).getMessage();
  }
}
