package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;
import software.wings.app.MainConfiguration;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.graphql.schema.type.trigger.TriggerActionController;
import software.wings.graphql.schema.type.trigger.TriggerConditionController;

@OwnedBy(CDC)
@UtilityClass
public class TriggerController {
  public static void populateTrigger(
      Trigger trigger, QLTriggerBuilder qlTriggerBuilder, MainConfiguration mainConfiguration, String accountId) {
    qlTriggerBuilder.id(trigger.getUuid())
        .name(trigger.getName())
        .description(trigger.getDescription())
        .condition(TriggerConditionController.populateTriggerCondition(trigger, mainConfiguration, accountId))
        .action(TriggerActionController.populateTriggerAction(trigger))
        .createdAt(trigger.getCreatedAt())
        .excludeHostsWithSameArtifact(trigger.isExcludeHostsWithSameArtifact())
        .createdBy(UserController.populateUser(trigger.getCreatedBy()));
  }
}
