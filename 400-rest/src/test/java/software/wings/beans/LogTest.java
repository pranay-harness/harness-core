/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.rule.OwnerRule.BRETT;

import static software.wings.beans.LogColor.Blue;
import static software.wings.beans.LogColor.Red;
import static software.wings.beans.LogColor.RedDark;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.END_MARK;
import static software.wings.beans.LogHelper.NO_FORMATTING;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogHelper.doneColoring;
import static software.wings.beans.LogWeight.Bold;
import static software.wings.beans.LogWeight.Normal;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LogTest extends CategoryTest {
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testLogColor() {
    String foo = color("foo", Yellow);

    assertThat(foo).isEqualTo("\033[0;" + Yellow.value + "m\033[40mfoo" + END_MARK);
    assertThat(doneColoring(foo)).isEqualTo("\033[0;" + Yellow.value + "m\033[40mfoo" + NO_FORMATTING);
  }
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testLogColorNested() {
    String bar = color("abc" + color("foo", Yellow) + "def", Blue);

    assertThat(bar).isEqualTo("\033[0;" + Blue.value + "m\033[40mabc\033[0;" + Yellow.value + "m\033[40mfoo\033[0;"
        + Blue.value + "m\033[40mdef" + END_MARK);
    assertThat(doneColoring(bar))
        .isEqualTo("\033[0;" + Blue.value + "m\033[40mabc\033[0;" + Yellow.value + "m\033[40mfoo\033[0;" + Blue.value
            + "m\033[40mdef" + NO_FORMATTING);
  }
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testLogColorMultiple() {
    String two = "a" + color("b", Yellow) + "c" + color("d", Blue) + "e";

    assertThat(two).isEqualTo(
        "a\033[0;" + Yellow.value + "m\033[40mb" + END_MARK + "c\033[0;" + Blue.value + "m\033[40md" + END_MARK + "e");

    String red = color(two, Red);

    assertThat(red).isEqualTo("\033[0;" + Red.value + "m\033[40ma\033[0;" + Yellow.value + "m\033[40mb\033[0;"
        + Red.value + "m\033[40mc\033[0;" + Blue.value + "m\033[40md\033[0;" + Red.value + "m\033[40me" + END_MARK);
    assertThat(doneColoring(red))
        .isEqualTo("\033[0;" + Red.value + "m\033[40ma\033[0;" + Yellow.value + "m\033[40mb\033[0;" + Red.value
            + "m\033[40mc\033[0;" + Blue.value + "m\033[40md\033[0;" + Red.value + "m\033[40me" + NO_FORMATTING);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testLogWeight() {
    String foo = color("foo", Yellow, Bold);

    assertThat(foo).isEqualTo("\033[1;" + Yellow.value + "m\033[40mfoo" + END_MARK);
    assertThat(doneColoring(foo)).isEqualTo("\033[1;" + Yellow.value + "m\033[40mfoo" + NO_FORMATTING);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testLogBackground() {
    String foo = color("foo", White, Normal, RedDark);

    assertThat(foo).isEqualTo("\033[0;" + White.value + "m\033[41mfoo" + END_MARK);
    assertThat(doneColoring(foo)).isEqualTo("\033[0;" + White.value + "m\033[41mfoo" + NO_FORMATTING);
  }
}
