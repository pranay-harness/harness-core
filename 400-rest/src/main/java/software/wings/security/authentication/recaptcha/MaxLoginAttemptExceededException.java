/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
