package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestAnnotationsCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  public DefaultConfiguration config() {
    DefaultConfiguration config = createModuleConfig(TestAnnotationsCheck.class);

    DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
    twConf.addChild(config);
    twConf.addAttribute("fileExtensions", "jv");

    return twConf;
  }

  @Test
  @Category(AbstractModuleTestSupport.class)
  public void testIssues() throws Exception {
    final String[] expected = {"4:3: Every test should be annotated with a category."};

    verify(config(), getPath("TestAnnotationsCheckIssues.jv"), expected);
  }

  @Test
  @Category(AbstractModuleTestSupport.class)
  public void testFalsePositive() throws Exception {
    final String[] expected = {};
    verify(config(), getPath("TestAnnotationsCheckNonIssues.jv"), expected);
  }
}