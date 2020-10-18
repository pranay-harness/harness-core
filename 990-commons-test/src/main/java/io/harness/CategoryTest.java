package io.harness;

import static org.junit.rules.RuleChain.outerRule;

import io.harness.rule.CategoryTimeoutRule;
import io.harness.rule.DistributeRule;
import io.harness.rule.OwnerRule;
import io.harness.rule.OwnerWatcherRule;
import io.harness.rule.RepeatRule;
import io.harness.rule.ThreadRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

@Slf4j
public class CategoryTest {
  @ClassRule public static ThreadRule threadRule = new ThreadRule();

  @Rule public OwnerWatcherRule ownerWatcherRule = new OwnerWatcherRule();
  @Rule public TestName testName = new TestName();
  @Rule public DistributeRule distributeRule = new DistributeRule();

  private RepeatRule repeatRule = new RepeatRule();

  @Rule
  public TestRule chain = outerRule(repeatRule).around(outerRule(new OwnerRule()).around(new CategoryTimeoutRule()));

  /**
   * Log test case name.
   */
  @Before
  public void logTestCaseName() {
    StringBuilder sb = new StringBuilder("Running test ").append(testName.getMethodName());

    int repetition = repeatRule.getRepetition();
    if (repetition > 0) {
      sb.append(" - ").append(repetition);
    }
    logger.info(sb.toString());
  }
}
