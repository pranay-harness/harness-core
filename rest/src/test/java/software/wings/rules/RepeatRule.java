package software.wings.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by anubhaw on 6/5/17.
 */

public class RepeatRule implements TestRule {
  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Repeat {
    int times();
    int successes() default - 1; // default value -1 gets converted to times() value.
  }

  private static class RepeatStatement extends Statement {
    private final int times;
    private final int successes;
    private final Statement statement;

    private RepeatStatement(int times, int successes, Statement statement) {
      this.times = times;
      this.successes = successes == -1 ? times : successes;
      this.statement = statement;
    }

    @Override
    public void evaluate() {
      int successfulCount = 0;
      for (int i = 0; i < times && successfulCount < successes; i++) {
        try {
          statement.evaluate();
          successfulCount++;
        } catch (Throwable throwable) {
          throwable.printStackTrace();
        }
      }
      if (successfulCount != successes) {
        throw new AssertionError(String.format(
            "Test failed more number of times than expected, Run count: Total: %s, Expected: %s, Actual: %s", times,
            successes, successfulCount));
      }
    }
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    Statement result = statement;
    Repeat repeat = description.getAnnotation(Repeat.class);
    if (repeat != null) {
      int times = repeat.times();
      int tillSuccessTimes = repeat.successes();
      result = new RepeatStatement(times, tillSuccessTimes, statement);
    }
    return result;
  }
}
