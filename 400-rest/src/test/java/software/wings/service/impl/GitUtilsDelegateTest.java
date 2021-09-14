/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.utils.GitUtilsDelegate;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitUtilsDelegateTest extends WingsBaseTest {
  @Inject private GitUtilsDelegate gitUtilsDelegate;

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetRequestDataFromFile() {
    String nonExistentPath = "/thisPathDoesNotExistOnDelegate/nonExistentFile.yaml";
    assertThatThrownBy(() -> gitUtilsDelegate.getRequestDataFromFile(nonExistentPath))
        .isInstanceOf(RuntimeException.class);
  }
}
