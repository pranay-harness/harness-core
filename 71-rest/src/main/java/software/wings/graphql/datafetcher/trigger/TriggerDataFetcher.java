package software.wings.graphql.datafetcher.trigger;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLTriggerQueryParameters;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

public class TriggerDataFetcher extends AbstractDataFetcher<QLTrigger, QLTriggerQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLTrigger fetch(QLTriggerQueryParameters parameters) {
    Trigger trigger = persistence.get(Trigger.class, parameters.getTriggerId());
    if (trigger == null) {
      return null;
    }

    QLTriggerBuilder qlTriggerBuilder = QLTrigger.builder();
    TriggerController.populateTrigger(trigger, qlTriggerBuilder);
    return qlTriggerBuilder.build();
  }
}
