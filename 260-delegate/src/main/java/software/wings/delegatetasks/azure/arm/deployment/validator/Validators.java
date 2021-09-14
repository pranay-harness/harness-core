/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.azure.arm.deployment.validator;

public class Validators {
  private Validators() {}

  public static <T> void validate(T t, Validator<T> validator) {
    validator.validate(t);
  }
}
