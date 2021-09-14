/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
