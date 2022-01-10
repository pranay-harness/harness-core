/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts;

import static io.harness.rule.OwnerRule.ARCHIT;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.beans.BuildDetailsInternal.BuildDetailsInternalBuilder;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorAscending;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class BuildDetailsInternalComparatorAscendingTest extends CategoryTest {
  private BuildDetailsInternalBuilder buildDetailsInternalBuilder =
      BuildDetailsInternal.builder().buildUrl("URL").uiDisplayName("DISPLAY_NAME");

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testShouldSortBuildDetailsInternalAscending() {
    List<BuildDetailsInternal> buildDetailsInternalList =
        asList(buildDetailsInternalBuilder.number("todolist-1.0-1.x86_64.rpm").build(),
            buildDetailsInternalBuilder.number("todolist-1.0-10.x86_64.rpm").build(),
            buildDetailsInternalBuilder.number("todolist-1.0-5.x86_64.rpm").build(),
            buildDetailsInternalBuilder.number("todolist-1.0-15.x86_64.rpm").build());
    assertThat(buildDetailsInternalList.stream()
                   .sorted(new BuildDetailsInternalComparatorAscending())
                   .collect(Collectors.toList()))
        .hasSize(4)
        .extracting(BuildDetailsInternal::getNumber)
        .containsSequence("todolist-1.0-1.x86_64.rpm", "todolist-1.0-5.x86_64.rpm", "todolist-1.0-10.x86_64.rpm",
            "todolist-1.0-15.x86_64.rpm");
  }
}
