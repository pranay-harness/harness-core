package io.harness;

import org.junit.runners.model.Statement;

public class NoopStatement extends Statement {
  @Override
  public void evaluate() throws Throwable {
    // noop
  }
}
