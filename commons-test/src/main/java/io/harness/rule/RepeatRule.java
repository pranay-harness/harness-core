package io.harness.rule;

import static java.lang.String.format;

import io.harness.rule.RepeatRule.RepeatStatement.RepeatStatementBuilder;
import lombok.Getter;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RepeatRule implements TestRule {
  @Getter private int repetition;

  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Repeat {
    int times();
    int successes() default - 1; // default value -1 gets converted to times() value.
  }

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
    final RepeatStatementBuilder builder = RepeatStatement.builder().statement(statement).parentRule(this);

    Repeat repeat = description.getAnnotation(Repeat.class);
    if (repeat != null) {
      final int times = repeat.times();
      final int successes = repeat.successes();
      return builder.times(times).successes(successes == -1 ? times : successes).build();
    }

    Category category = description.getAnnotation(Category.class);
    if (category != null) {
      return builder.times(3).successes(1).timeoutOnly(true).build();
    }

    return statement;
  }
  }
