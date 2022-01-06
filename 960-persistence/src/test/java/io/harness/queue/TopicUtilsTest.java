/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.queue;

import static io.harness.rule.OwnerRule.GEORGE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TopicUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCombineElements() {
    assertThat(TopicUtils.combineElements(null)).isNull();
    assertThat(TopicUtils.combineElements(asList())).isNull();

    assertThat(TopicUtils.combineElements(asList("element"))).isEqualTo("element");
    assertThat(TopicUtils.combineElements(asList("element1", "element2"))).isEqualTo("element1;element2");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testResolveExpressionIntoListOfTopics() {
    assertThat(TopicUtils.resolveExpressionIntoListOfTopics(null)).isNull();
    assertThat(TopicUtils.resolveExpressionIntoListOfTopics(asList())).isNull();

    assertThat(TopicUtils.resolveExpressionIntoListOfTopics(asList(asList("element")))).containsExactly("element");

    assertThat(TopicUtils.resolveExpressionIntoListOfTopics(asList(asList("element1", "element2"))))
        .containsExactly("element1", "element2");

    assertThat(TopicUtils.resolveExpressionIntoListOfTopics(
                   asList(asList("prefix"), asList("element1", "element2"), asList("suffix"))))
        .containsExactly("prefix;element1;suffix", "prefix;element2;suffix");

    assertThat(TopicUtils.resolveExpressionIntoListOfTopics(asList(asList("prefix"), asList("suffix"))))
        .containsExactly("prefix;suffix");
  }
}
