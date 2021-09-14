/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.rule;

import static java.lang.Thread.activeCount;
import static org.assertj.core.api.Assertions.fail;

import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@Slf4j
public class ThreadControlRule implements TestRule {
  @Override
  public Statement apply(Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        int activeThreadsBeforeTheTest = activeCount();
        log.info("Threads before the test: {}", activeThreadsBeforeTheTest);
        try {
          statement.evaluate();
        } finally {
          int activeThreadsAfterTheTest = activeCount();
          log.info("Threads after the test: {}", activeThreadsAfterTheTest);
          if (activeThreadsAfterTheTest > activeThreadsBeforeTheTest) {
            fail(String.format("There are %d threads more at the end of the test",
                activeThreadsAfterTheTest - activeThreadsBeforeTheTest));
          }
        }
      }
    };
  }
}
