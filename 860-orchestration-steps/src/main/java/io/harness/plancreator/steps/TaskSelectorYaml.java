package io.harness.plancreator.steps;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.TaskSelector;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
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
