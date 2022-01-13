/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.rule.OwnerRule.SRINIVAS;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.command.CommandUnitType;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Created by peeyushaggarwal on 6/6/16.
 */
@RunWith(JUnitParamsRunner.class)
public class CommandUnitTypeTest extends CategoryTest {
  private Object[][] getData() {
    Object[][] data = new Object[CommandUnitType.values().length][1];

    for (int i = 0; i < CommandUnitType.values().length; i++) {
      data[i][0] = UPPER_UNDERSCORE.to(UPPER_CAMEL, CommandUnitType.values()[i].name());
    }
    return data;
  }

  /**
   * Should create new instance for.
   *
   * @param commandUnitTypeName the command unit type name
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: This test is failing in bazel. Changes are required from the owner to make it work in bazel")
  @Parameters(method = "getData")
  public void shouldCreateNewInstanceFor(String commandUnitTypeName) throws Exception {
    CommandUnitType commandUnitType = CommandUnitType.valueOf(UPPER_CAMEL.to(UPPER_UNDERSCORE, commandUnitTypeName));
    assertThat(commandUnitType).isNotNull();
    assertThat(commandUnitType.newInstance("")).isNotNull();
  }
}
