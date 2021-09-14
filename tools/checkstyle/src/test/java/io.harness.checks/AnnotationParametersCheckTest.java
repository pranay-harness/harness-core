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

public class AnnotationParametersCheckTest extends AbstractModuleTestSupport {
  @Override
  protected String getPackageLocation() {
    return "io.harness.checks";
  }

  public DefaultConfiguration config() {
    DefaultConfiguration config = createModuleConfig(AnnotationParametersCheck.class);

    DefaultConfiguration twConf = createModuleConfig(TreeWalker.class);
    twConf.addChild(config);
    twConf.addAttribute("fileExtensions", "jv");

    return twConf;
  }

  @Test
  public void testIssues() throws Exception {
    final String[] expected = {
        "8:1: The annotation FieldNameConstants does not meet expectation: Parameter innerTypeName is not provided.",
        "11:3: The annotation Ignore does not meet expectation: The default parameter value does not match the expected pattern '.{25,110}'.",
        "16:3: The annotation Ignore does not meet expectation: The default parameter value does not match the expected pattern '.{25,110}'."};

    verify(config(), getPath("AnnotationParametersCheckIssues.jv"), expected);
  }

  @Test
  public void testFalsePositive() throws Exception {
    final String[] expected = {};
    verify(config(), getPath("AnnotationParametersCheckNonIssues.jv"), expected);
  }
}
