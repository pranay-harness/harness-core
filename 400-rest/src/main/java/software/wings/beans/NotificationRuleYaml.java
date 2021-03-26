package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class NotificationRuleYaml extends BaseYaml {
  private List<String> conditions = new ArrayList<>();
  private String executionScope;
  private List<String> notificationGroups = new ArrayList<>();
  private boolean notificationGroupAsExpression;
  private boolean userGroupAsExpression;
  private String userGroupExpression;

  private List<String> userGroupIds = new ArrayList<>();

  @Builder
  public NotificationRuleYaml(List<String> conditions, String executionScope, List<String> notificationGroups,
      boolean notificationGroupAsExpression, boolean userGroupAsExpression, List<String> userGroupIds,
      String userGroupExpression) {
    this.conditions = conditions;
    this.executionScope = executionScope;
    this.notificationGroups = notificationGroups;
    this.notificationGroupAsExpression = notificationGroupAsExpression;
    this.userGroupAsExpression = userGroupAsExpression;
    this.userGroupIds = userGroupIds;
    this.userGroupExpression = userGroupExpression;
  }
}
