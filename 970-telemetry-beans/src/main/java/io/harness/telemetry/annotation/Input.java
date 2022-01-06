/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry.annotation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * Providing two options for the input.
 * Choose either a string value,
 * or the argument(index starting from 0) in the method at runtime. if argument value is null will be omitted.
 *
 * If set both value will be used.
 */
@OwnedBy(HarnessTeam.GTM)
public @interface Input {
  String value() default "";
  int argumentIndex() default - 1;
}
