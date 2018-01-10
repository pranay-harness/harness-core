package software.wings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.rules.CategoryTimeoutRule;
import software.wings.rules.RepeatRule;

public class BasicTest {
  @Rule public TestName testName = new TestName();

  private RepeatRule repeatRule = new RepeatRule();

  @Rule public TestRule chain = RuleChain.outerRule(repeatRule).around(new CategoryTimeoutRule());

  /**
   * Log test case name.
   */
  @Before
  public void logTestCaseName() {
    if (repeatRule.getRepetition() == 0) {
      System.out.println(String.format("Running test %s", testName.getMethodName()));
    } else {
      System.out.println(String.format("Running test %s - %s", testName.getMethodName(), repeatRule.getRepetition()));
    }
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
