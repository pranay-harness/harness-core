/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
