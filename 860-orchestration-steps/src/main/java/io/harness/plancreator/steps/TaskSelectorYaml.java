package io.harness.plancreator.steps;

import io.harness.delegate.TaskSelector;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class TaskSelectorYaml {
  String delegateSelectors;
  TaskSelectorYaml(String delegateSelectors) {
    this.delegateSelectors = delegateSelectors;
  }
  public static TaskSelector toTaskSelector(TaskSelectorYaml taskSelectorYaml) {
    return TaskSelector.newBuilder().setSelector(taskSelectorYaml.delegateSelectors).build();
  }
  public static List<TaskSelector> toTaskSelector(List<TaskSelectorYaml> taskSelectorYaml) {
    return taskSelectorYaml.stream().map(TaskSelectorYaml::toTaskSelector).collect(Collectors.toList());
  }
}
