package io.harness.rule;

import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@Slf4j
public class ThreadRule implements TestRule {
  @Override
  public Statement apply(Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        int startThreads = Thread.activeCount();
        statement.evaluate();
        int endThreads = Thread.activeCount();

        if (endThreads > startThreads) {
          String message = String.format("%d - threads leaked", endThreads - startThreads);
          logger.error(message);
          if ("enforce".equals(System.getenv("THREAD_LEAKS"))) {
            throw new RuntimeException(message);
          }
        }
      }
    };
  }
}