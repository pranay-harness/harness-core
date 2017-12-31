package software.wings.rules;

import lombok.Builder;
import lombok.Getter;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;
import software.wings.exception.CauseCollection;
import software.wings.rules.RepeatRule.RepeatStatement.RepeatStatementBuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by anubhaw on 6/5/17.
 */

public class RepeatRule implements TestRule {
  @Getter private int repetition;

  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Repeat {
    int times();
    int successes() default - 1; // default value -1 gets converted to times() value.
  }

  @Builder
  protected static class RepeatStatement extends Statement {
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
      // Use cause collection to track the reasons to fail, but do not print until we are sure that it will fail.
      CauseCollection causeCollection = new CauseCollection();
      int successfulCount = 0;
      for (parentRule.repetition = 1; parentRule.repetition <= times && successfulCount < successes;
           parentRule.repetition++) {
        try {
          // Evaluate the original statement
          statement.evaluate();
          successfulCount++;
        } catch (TestTimedOutException ex) {
          // We track timeouts in every scenario
          causeCollection.addCause(ex);
        } catch (Throwable throwable) {
          if (timeoutOnly) {
            throw throwable;
          }
          causeCollection.addCause(throwable);
        }
      }
      if (successfulCount != successes) {
        throw new AssertionError(
            String.format(
                "Test failed more number of times than expected, Run count: Total: %s, Expected: %s, Actual: %s", times,
                successes, successfulCount),
            causeCollection.getCause());
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
