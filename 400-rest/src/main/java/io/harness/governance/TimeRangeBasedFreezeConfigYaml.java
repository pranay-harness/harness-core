package io.harness.governance;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.resources.stats.model.TimeRangeYaml;
import software.wings.service.impl.yaml.handler.governance.GovernanceFreezeConfigYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("TIME_RANGE_BASED_FREEZE_CONFIG")
public final class TimeRangeBasedFreezeConfigYaml extends GovernanceFreezeConfigYaml {
  private String name;
  private String description;
  private boolean applicable;
  private List<String> userGroups;
  private List<ApplicationFilterYaml> appSelections;
  private TimeRangeYaml timeRange;

  @Builder
  public TimeRangeBasedFreezeConfigYaml(String type, String name, String description, boolean applicable,
      List<String> userGroups, List<ApplicationFilterYaml> appSelections, TimeRangeYaml timeRange) {
    super(type);
    setName(name);
    setDescription(description);
    setApplicable(applicable);
    setUserGroups(userGroups);
    setAppSelections(appSelections);
    setTimeRange(timeRange);
  }

  public TimeRangeBasedFreezeConfigYaml() {
    super("TIME_RANGE_BASED_FREEZE_CONFIG");
  }
}
