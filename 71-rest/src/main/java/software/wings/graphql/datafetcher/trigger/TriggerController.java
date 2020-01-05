package software.wings.graphql.datafetcher.trigger;

import lombok.experimental.UtilityClass;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;

@UtilityClass
public class TriggerController {
  public static void populateTrigger(Trigger trigger, QLTriggerBuilder qlTriggerBuilder) {
    qlTriggerBuilder.id(trigger.getUuid())
        .name(trigger.getName())
        .description(trigger.getDescription())
        .createdAt(trigger.getCreatedAt())
        .createdBy(UserController.populateUser(trigger.getCreatedBy()));
  }
}
