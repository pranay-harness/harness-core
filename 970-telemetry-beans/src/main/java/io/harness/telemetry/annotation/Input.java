/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
