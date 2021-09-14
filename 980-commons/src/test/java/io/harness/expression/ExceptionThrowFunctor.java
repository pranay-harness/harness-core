/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression;

import io.harness.exception.FunctorException;

public class ExceptionThrowFunctor implements ExpressionFunctor {
  public void throwException() {
    throw new FunctorException("My Exception");
  }
}
