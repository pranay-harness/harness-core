/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.TaskSelector;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public class TaskSelectorYaml {
  String delegateSelectors;
  public TaskSelectorYaml(String delegateSelectors) {
    this.delegateSelectors = delegateSelectors;
  }
  public static TaskSelector toTaskSelector(TaskSelectorYaml taskSelectorYaml) {
    return TaskSelector.newBuilder().setSelector(taskSelectorYaml.delegateSelectors).build();
  }
  public static List<TaskSelector> toTaskSelector(List<TaskSelectorYaml> taskSelectorYaml) {
    return taskSelectorYaml.stream().map(TaskSelectorYaml::toTaskSelector).collect(Collectors.toList());
  }
}
