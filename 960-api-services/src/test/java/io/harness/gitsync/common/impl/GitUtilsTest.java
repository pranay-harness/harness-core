/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.gitsync.common.impl;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void convertToUrlWithGitTest() {
    String modifiedPath = GitUtils.convertToUrlWithGit("https://abc.git///");
    assertThat(modifiedPath).isEqualTo("https://abc");

    modifiedPath = GitUtils.convertToUrlWithGit("https://abc///");
    assertThat(modifiedPath).isEqualTo("https://abc");
  }
}
