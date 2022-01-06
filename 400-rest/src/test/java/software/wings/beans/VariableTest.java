/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static software.wings.beans.Variable.VariableBuilder.aVariable;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VariableTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void but() {
    Variable src = aVariable()
                       .name("var")
                       .value("foo")
                       .type(VariableType.TEXT)
                       .description("desc")
                       .mandatory(true)
                       .metadata(ImmutableMap.of("key", "value"))
                       .allowMultipleValues(true)
                       .allowedValues("foo, bar")
                       .allowedList(Arrays.asList("a", "b"))
                       .build();

    Variable dst = src.but().value("new-value").build();

    assertThat(dst).isEqualToIgnoringGivenFields(src, "value");
    assertThat(dst.getValue()).isEqualTo("new-value");
  }
}
