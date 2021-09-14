/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.rule;

import io.harness.factory.ClosingFactory;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class LifecycleRule implements TestRule {
  private ClosingFactory closingFactory = new ClosingFactory();

  public ClosingFactory getClosingFactory() {
    return closingFactory;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
        } finally {
          closingFactory.stopServers();
        }
      }
    };
  }
}
