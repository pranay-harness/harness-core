/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.annotation;

import io.harness.annotations.retry.IMethodWrapper;

public class MockMethodWrapperImpl implements IMethodWrapper<Object> {
  @Override
  public Object execute() throws Throwable {
    return null;
  }
}
