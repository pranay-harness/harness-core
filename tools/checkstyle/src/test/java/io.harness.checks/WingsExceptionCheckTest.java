/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks;

import com.puppycrawl.tools.checkstyle.AbstractModuleTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.TreeWalker;
import org.junit.Test;

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
  public void testWingsExceptionIssues() throws Exception {
    final String[] expected = {
        "5:30: Do not instantiate WingsException directly - instead use InvalidRequestException.",
        "6:40: Do not instantiate WingsException directly - instead use InvalidRequestException."};

    verify(config(), getPath("WingsExceptionCheckIssues.jv"), expected);
  }

  @Test
  public void testFalsePositive() throws Exception {
    final String[] expected = {};
    verify(config(), getPath("WingsExceptionCheckNonIssues.jv"), expected);
  }
}
