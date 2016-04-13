package software.wings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRule;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.rules.WingsRule;

/**
 * Created by peeyushaggarwal on 4/5/16.
 */
public class WingsBaseTest {
  @Rule public TestName testName = new TestName();

  @Before
  public void logTestCaseName() {
    System.out.println(String.format("Running test %s", testName.getMethodName()));
  }

  @Rule public WingsRule wingsRule = new WingsRule();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  protected Logger logger() {
    return LoggerFactory.getLogger(getClass());
  }
}
