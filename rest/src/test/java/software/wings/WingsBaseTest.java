package software.wings;

import io.harness.CategoryTest;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.rules.WingsRule;

/**
 * Created by anubhaw on 4/28/16.
 */
public abstract class WingsBaseTest extends CategoryTest {
  // I am not absolutely sure why, but there is dependency between wings io.harness.rule and
  // MockitoJUnit io.harness.rule and they have to be listed in these order
  @Rule public WingsRule wingsRule = new WingsRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
}
