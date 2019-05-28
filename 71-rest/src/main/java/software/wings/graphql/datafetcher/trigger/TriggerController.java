package software.wings.graphql.datafetcher.trigger;

import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;

public class TriggerController {
  public static void populateTrigger(Trigger trigger, QLTriggerBuilder qlTriggerBuilder) {
    qlTriggerBuilder.id(trigger.getUuid())
        .name(trigger.getName())
        .description(trigger.getDescription())
        .createdAt(GraphQLDateTimeScalar.convert(trigger.getCreatedAt()))
        .createdBy(UserController.populateUser(trigger.getCreatedBy()));
  }
}
