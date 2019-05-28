package software.wings.graphql.datafetcher.trigger;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.query.QLTriggersQueryParameters;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.graphql.schema.type.trigger.QLTriggerConnection;
import software.wings.graphql.schema.type.trigger.QLTriggerConnection.QLTriggerConnectionBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class TriggerConnectionDataFetcher
    extends AbstractConnectionDataFetcher<QLTriggerConnection, QLTriggersQueryParameters> {
  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN, action = Action.READ)
  protected QLTriggerConnection fetchConnection(QLTriggersQueryParameters qlQuery) {
    final Query<Trigger> query = persistence.createAuthorizedQuery(Trigger.class)
                                     .filter(TriggerKeys.appId, qlQuery.getApplicationId())
                                     .order(Sort.descending(TriggerKeys.createdAt));

    QLTriggerConnectionBuilder qlTriggerConnectionBuilder = QLTriggerConnection.builder();
    qlTriggerConnectionBuilder.pageInfo(populate(qlQuery, query, trigger -> {
      QLTriggerBuilder builder = QLTrigger.builder();
      TriggerController.populateTrigger(trigger, builder);
      qlTriggerConnectionBuilder.node(builder.build());
    }));

    return qlTriggerConnectionBuilder.build();
  }
}
