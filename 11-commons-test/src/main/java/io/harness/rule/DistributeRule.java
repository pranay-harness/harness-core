package io.harness.rule;

import static org.junit.Assume.assumeTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.powermock.modules.junit4.PowerMockRunner;

@Slf4j
public class DistributeRule implements TestRule {
  private static int CURRENT_WORKER;
  private static int TOTAL_WORKERS;

  static {
    final String envWorker = System.getenv("DISTRIBUTE_TESTING_WORKER");
    final String envWorkers = System.getenv("DISTRIBUTE_TESTING_WORKERS");
    if (envWorker != null && envWorkers != null) {
      final int worker = Integer.parseInt(envWorker);
      final int workers = Integer.parseInt(envWorkers);

      if (worker >= 0 && worker < workers) {
        CURRENT_WORKER = worker;
        TOTAL_WORKERS = workers;
        logger.info("Distributed test execution on {} workers", TOTAL_WORKERS);
      }
    }
  }

  private int worker(Description description) {
    if (TOTAL_WORKERS == 0) {
      return 0;
    }

    final String identifier = description.getDisplayName();
    int hash = identifier.hashCode();
    if (hash < 0) {
      hash = -(hash + 1);
    }
    return hash % TOTAL_WORKERS;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        final int worker = worker(description);

        // PowerMockRunner does not support assume
        final RunWith annotation = description.getTestClass().getAnnotation(RunWith.class);
        if (annotation != null && annotation.value() == PowerMockRunner.class) {
          if (worker == CURRENT_WORKER) {
            statement.evaluate();
          }
        } else {
          assumeTrue(String.format("This test will be executed in worker %d", worker), worker == CURRENT_WORKER);
          statement.evaluate();
        }
      }
    };
  }
}
