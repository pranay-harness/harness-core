/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.expression.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;

@OwnedBy(CDC)
public class DateTimeFunctor implements ExpressionFunctor {
  public CustomDate currentDate() {
    return new CustomDate();
  }

  public CustomDateTime currentTime() {
    return new CustomDateTime();
  }
}
