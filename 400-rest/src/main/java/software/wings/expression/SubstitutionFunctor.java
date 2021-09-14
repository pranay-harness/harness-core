/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.LateBindingMap;

import lombok.Builder;

@OwnedBy(CDC)
@Builder
public class SubstitutionFunctor extends LateBindingMap {
  public Object startTailLogVerification(String... args) {
    return substitute("harness_utils_start_tail_log_verification", args);
  }

  public Object waitForTailLogVerification(String... args) {
    return substitute("harness_utils_wait_for_tail_log_verification", args);
  }

  private String substitute(String functionName, String... args) {
    StringBuilder output = new StringBuilder(functionName);
    for (String arg : args) {
      output.append(" \"").append(arg).append('"');
    }
    return output.toString();
  }

  @Override
  public Object get(Object key) {
    throw new UnsupportedOperationException("Not supported");
  }
}
