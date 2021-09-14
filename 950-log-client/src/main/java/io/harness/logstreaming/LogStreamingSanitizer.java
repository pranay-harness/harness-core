/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logstreaming;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.expression.SecretString.SECRET_MASK;

import static org.apache.commons.lang3.StringUtils.replaceEach;

import java.util.ArrayList;
import java.util.Set;
import lombok.Builder;

@Builder
public class LogStreamingSanitizer {
  private final Set<String> secrets;

  public void sanitizeLogMessage(LogLine logLine) {
    if (isEmpty(secrets)) {
      return;
    }

    ArrayList<String> secretMasks = new ArrayList<>();
    ArrayList<String> secretValues = new ArrayList<>();
    for (String secret : secrets) {
      secretMasks.add(SECRET_MASK);
      secretValues.add(secret);
    }
    String sanitizedLogMessage =
        replaceEach(logLine.getMessage(), secretValues.toArray(new String[] {}), secretMasks.toArray(new String[] {}));

    logLine.setMessage(sanitizedLogMessage);
  }
}
