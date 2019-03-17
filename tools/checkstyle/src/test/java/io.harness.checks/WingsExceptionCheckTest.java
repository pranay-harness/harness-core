package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WingsExceptionCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  public DefaultConfiguration config() {
    DefaultConfiguration config = createModuleConfig(WingsExceptionCheck.class);

    DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
    twConf.addChild(config);
    twConf.addAttribute("fileExtensions", "jv");

    return twConf;
  }

  @Test
  @Category(AbstractModuleTestSupport.class)
  public void testWingsExceptionIssues() throws Exception {
    final String[] expected = {"5:30: Do not instantiate WingsException directly - instead use InvalidRequestException",
        "6:40: Do not instantiate WingsException directly - instead use InvalidRequestException"};

    verify(config(), getPath("WingsExceptionCheckIssues.jv"), expected);
  }

  @Test
  @Category(AbstractModuleTestSupport.class)
  public void testFalsePositive() throws Exception {
    final String[] expected = {};
    verify(config(), getPath("WingsExceptionCheckNonIssues.jv"), expected);
  }
}