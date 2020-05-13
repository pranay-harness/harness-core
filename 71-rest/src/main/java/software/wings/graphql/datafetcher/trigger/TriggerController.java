package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;

@OwnedBy(CDC)
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
