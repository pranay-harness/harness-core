/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.common;

import static io.harness.rule.OwnerRule.SRINIVAS;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AlphanumComparatorTest extends CategoryTest {
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldSortAscendingOrder() {
    List<String> values = asList("todolist-1.0-10.x86_64.rpm", "todolist-1.0-1.x86_64.rpm", "todolist-1.0-3.x86_64.rpm",
        "todolist-1.0-2.x86_64.rpm");
    values = values.stream().sorted(new AlphanumComparator()).collect(toList());

    assertThat(values).hasSize(4).containsSequence("todolist-1.0-1.x86_64.rpm", "todolist-1.0-2.x86_64.rpm",
        "todolist-1.0-3.x86_64.rpm", "todolist-1.0-10.x86_64.rpm");
  }
}
