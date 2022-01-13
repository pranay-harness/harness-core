/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.rule;

import static java.lang.String.format;

import io.harness.rule.RepeatRule.RepeatStatement.RepeatStatementBuilder;

import lombok.Getter;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

/*
I. Do not use Repeat to execute the same logic more than once. Instead, just create inside the test logic to repeat the
sensitive section. The test frameworks do a lot of work to prepare the test for execution, we should not need to do
this again and again. Not to mention that this is not always safe to do when integrating with PowerMock and similar.

II. Do not use test annotation @Repeat with value for successes.

Using this field does not resolve test flakiness. It at best simply reduces the likelihood of the issue to surface.

Instead, use one of the following approaches to resolve the flakiness:

Use junit.Assume.assumeTrue to confirm that every precondition for your test to succeed is met. If your tests
depend on infrastructure make sure it is available in order to execute the test. The infrastructure should be monitored
from the outside system for availability, not from the tests.
Use retry from inside the test. You should not use the Test framework to repeat the test if it requires
experimentation. Encode this logic inside the test.
Pool until the expected condition is met with a proper timeout.
 */
@Deprecated
public class RepeatRule implements TestRule {
  @Getter private int repetition;

  // TODO: find out why Builder and Statement require full path to compile
  @lombok.Builder
  protected static class RepeatStatement extends org.junit.runners.model.Statement {
    private RepeatRule parentRule;

    // Defines up to how many times to repeat the tests
    private int times;

    // Defines on how many successful executions to stop
    private int successes;

    // Repeat logic applies only for timeout issues
    private boolean timeoutOnly;

    // The original statement
    private Statement statement;

    @Override
    public void evaluate() throws Throwable {
      if (parentRule == null) {
        return;
      }

      // There might be many different reasons for test to fail, but that is unlikely.
      // Providing the last one is good enough. When it is resolved and there is another - it will became visible.
      Throwable lastException = null;
      int successfulCount = 0;
      for (parentRule.repetition = 1; parentRule.repetition <= times && successfulCount < successes;
           parentRule.repetition++) {
        try {
          // Evaluate the original statement
          statement.evaluate();
          successfulCount++;
        } catch (TestTimedOutException ex) {
          // We track timeouts in every scenario
          lastException = ex;
        } catch (Exception exception) {
          if (timeoutOnly) {
            throw exception;
          }
          lastException = exception;
        }
      }
      if (successfulCount != successes) {
        throw new AssertionError(
            format("Test failed more number of times than expected, Run count: Total: %s, Expected: %s, Actual: %s",
                times, successes, successfulCount),
            lastException);
      }
    }
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    if (statement instanceof RepeatStatement) {
      return statement;
    }

    final RepeatStatementBuilder builder = RepeatStatement.builder().statement(statement).parentRule(this);

    Repeat repeat = description.getAnnotation(Repeat.class);
    if (repeat != null) {
      final int times = repeat.times();
      final int successes = repeat.successes();
      return builder.times(times).successes(successes == -1 ? times : successes).build();
    }

    return statement;
  }
}
