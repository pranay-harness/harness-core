/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.azure.arm.deployment.validator;

@FunctionalInterface
public interface Validator<T> {
  void validate(T t);
}
