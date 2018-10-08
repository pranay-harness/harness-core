package io.harness;

import static org.junit.rules.RuleChain.outerRule;

import io.harness.rule.CategoryTimeoutRule;
import io.harness.rule.OwnerRule;
import io.harness.rule.RepeatRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryTest {
  private static final Logger logger = LoggerFactory.getLogger(CategoryTest.class);

  @Rule public TestName testName = new TestName();

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

  /**
   * Log.
   *
   * @return the logger
   */
  protected Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
