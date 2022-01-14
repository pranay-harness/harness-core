package io.harness.annotation;

import io.harness.annotations.retry.IMethodWrapper;

public class MockMethodWrapperImpl implements IMethodWrapper<Object> {
  @Override
  public Object execute() throws Throwable {
    return null;
  }
}
