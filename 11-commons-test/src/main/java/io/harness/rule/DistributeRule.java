package io.harness.rule;

import static org.junit.Assume.assumeTrue;

import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

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

    logger.info("No distribution");
  }

  private int worker(Description description) {
    if (TOTAL_WORKERS == 0) {
      return 0;
    }
    int hash = description.getDisplayName().hashCode();
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
        assumeTrue(String.format("This test will be executed in worker %d", worker), worker == CURRENT_WORKER);
        statement.evaluate();
      }
    };
  }
}
