package software.wings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by anubhaw on 4/28/16.
 */
public abstract class WingsBaseTest {
  @Rule public TestName testName = new TestName();

  @Before
  public void logTestCaseName() {
    System.out.println(String.format("Running test %s", testName.getMethodName()));
  }

  protected Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
