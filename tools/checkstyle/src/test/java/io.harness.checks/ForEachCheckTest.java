package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ForEachCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  public DefaultConfiguration config() {
    DefaultConfiguration config = createModuleConfig(ForEachCheck.class);

    DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
    twConf.addChild(config);
    twConf.addAttribute("fileExtensions", "jv");

    return twConf;
  }

  @Test
  @Category(AbstractModuleTestSupport.class)
  public void testIssues() throws Exception {
    final String[] expected = {"5:19: Collection forEach is faster than stream forEach - use it instead.",
        "6:20: EntrySet forEach can be replaced with direct map forEach in (k, v)."};

    verify(config(), getPath("ForEachCheckIssues.jv"), expected);
  }

  @Test
  @Category(AbstractModuleTestSupport.class)
  public void testFalsePositive() throws Exception {
    final String[] expected = {};
    verify(config(), getPath("ForEachCheckNonIssues.jv"), expected);
  }
}