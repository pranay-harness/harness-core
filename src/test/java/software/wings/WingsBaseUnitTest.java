package software.wings;

import static software.wings.rules.WingsRule.TestType.UNIT;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.rules.WingsRule;

/**
 * Created by peeyushaggarwal on 4/5/16.
 */
public class WingsBaseUnitTest extends WingsBaseTest {
  @Rule public WingsRule wingsRule = new WingsRule(UNIT);
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
}
