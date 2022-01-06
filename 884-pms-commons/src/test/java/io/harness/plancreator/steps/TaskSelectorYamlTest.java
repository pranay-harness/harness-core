/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.TaskSelector;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class TaskSelectorYamlTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToTaskSelector() {
    TaskSelectorYaml taskSelectorYaml = new TaskSelectorYaml("selectMe");
    TaskSelector taskSelector = TaskSelectorYaml.toTaskSelector(taskSelectorYaml);
    assertThat(taskSelector.getSelector()).isEqualTo("selectMe");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToTaskSelectorForList() {
    TaskSelectorYaml taskSelectorYaml0 = new TaskSelectorYaml("selectMe0");
    TaskSelectorYaml taskSelectorYaml1 = new TaskSelectorYaml("selectMe1");
    TaskSelectorYaml taskSelectorYaml2 = new TaskSelectorYaml("selectMe2");
    TaskSelectorYaml taskSelectorYaml3 = new TaskSelectorYaml("selectMe3");
    List<TaskSelectorYaml> taskSelectorYamlList =
        Arrays.asList(taskSelectorYaml0, taskSelectorYaml1, taskSelectorYaml2, taskSelectorYaml3);
    List<TaskSelector> taskSelectors = TaskSelectorYaml.toTaskSelector(taskSelectorYamlList);
    assertThat(taskSelectors).hasSize(4);
    for (int i = 0; i < 4; i++) {
      assertThat(taskSelectors.get(i).getSelector()).isEqualTo("selectMe" + i);
    }
  }
}
