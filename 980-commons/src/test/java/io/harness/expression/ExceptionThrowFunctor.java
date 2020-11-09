package io.harness.expression;

import io.harness.exception.FunctorException;

public class ExceptionThrowFunctor implements ExpressionFunctor {
  public void throwException() {
    throw new FunctorException("My Exception");
  }
}
