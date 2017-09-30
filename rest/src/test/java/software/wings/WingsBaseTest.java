package software.wings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.rules.RepeatRule;
import software.wings.rules.WingsRule;

/**
 * Created by anubhaw on 4/28/16.
 */
public abstract class WingsBaseTest {
  protected static final String AWS_CREDENTIALS_LOCATION = "AWS_CREDENTIALS_LOCATION";

  /**
   * The Test name.
   */
  @Rule public TestName testName = new TestName();
  /**
   * The Wings rule.
   */
  @Rule public WingsRule wingsRule = new WingsRule();
  /**
   * The Mockito rule.
   */
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule public RepeatRule repeatRule = new RepeatRule();

  /**
   * Log test case name.
   */
  @Before
  public void logTestCaseName() {
    System.out.println(String.format("Running test %s", testName.getMethodName()));
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
