/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface Expression {
  String ALLOW_SECRETS = "ALLOW_SECRETS";
  String DISALLOW_SECRETS = "DISALLOW_SECRETS";

  enum SecretsMode { ALLOW_SECRETS, DISALLOW_SECRETS }

  String value();
}
